package ingredients.overlay.security;

import java.util.Set;
import java.util.TreeSet;

import messages.Message;
import messages.OmissionDefense.BlackListMessage;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class MalBlackListIngredient extends BlackListIngredient {
  @Override public String getMessageTag() {
    return alg.getMessageTag() + "-" + "BlackListBehavior";
  }
  
  // A malicious node wishes to disconnect from other malicious neighbors in
  // order to connect to more legitimate neighbors instead and cause more damage
  // to the system.
  @Override public void nextCycle() {
    super.nextCycle();
  }
  
  // A malicious node will add a node to its black list only in case it is not
  // one of its collaborating nodes.
  @Override protected void addToBlackList(final NodeAddress node) {
    final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
    final int nodeToRemove = Common.currentConfiguration.getNodeGroup(node.toString());
    if (myGroup != nodeToRemove) {
      blackListNodes.add(node);
    }
  }
  
  // A malicious node will add nodes that are banned by the source only if they
  // are not from the same group as them.
  @Override public void handleMessage(final Message message) {
    if (message instanceof BlackListMessage) {
      final BlackListMessage blm = (BlackListMessage) message;
      assert blm.blamingNode.equals(client.network.getServerNode());
      final boolean simulation = true;
      // If I'm in simulation mode add this banned node to banned nodes only if
      // it from other group but still inform all your neighbors about this
      // banned node.
      if (simulation) {
        if (sourceBannedNodes.contains(blm.blamedNode)) {
          return;
        }
        final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
        final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blm.blamedNode.toString());
        if (myGroup != blamedNodeGroup) {
          sourceBannedNodes.add(blm.blamedNode);
          blackListNodes.add(blm.blamedNode);
        }
        final Set<NodeAddress> neighborsToInform = new TreeSet<NodeAddress>(alg.getNeighbors());
        neighborsToInform.remove(blm.sourceId);
        for (final NodeAddress neighbor : neighborsToInform) {
          client.network.send(new BlackListMessage(getMessageTag(), client.network.getAddress(), neighbor, blm.blamingNode,
              blm.blamedNode));
        }
      } else { // If we are not in simulation the malicious nodes can intercept
        // these ban message and not deliver them to the rest of the
        // network.
        final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
        final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blm.blamedNode.toString());
        if (sourceBannedNodes.contains(blm.blamedNode) || myGroup == blamedNodeGroup) {
          return;
        }
        sourceBannedNodes.add(blm.blamedNode);
        blackListNodes.add(blm.blamedNode);
        // Inform all my group neighbors only if I got this message from a node
        // which is not in my group
        if (Common.currentConfiguration.getNodeGroup(blm.sourceId.toString()) == myGroup) {
          return;
        }
        final Set<NodeAddress> nodesToInform = new TreeSet<NodeAddress>(Common.currentConfiguration.getGroupMemebers(myGroup));
        nodesToInform.remove(client.network.getAddress());
        for (final NodeAddress nodeToInform : nodesToInform) {
          client.network.send(new BlackListMessage(getMessageTag(), client.network.getAddress(), nodeToInform, blm.blamingNode,
              blm.blamedNode));
        }
      }
    }
  }
}
