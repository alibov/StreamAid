package ingredients.overlay.security;

import ingredients.AbstractIngredient;

import java.util.Set;
import java.util.TreeSet;

import messages.Message;
import messages.OmissionDefense.BlackListMessage;
import modules.overlays.OverlayModule;
import experiment.frameworks.NodeAddress;

public class BlackListIngredient extends AbstractIngredient<OverlayModule<?>> {
  public BlackListIngredient() {
    super(null);
  }
  
  // Nodes that are in my local black list.
  protected final Set<NodeAddress> blackListNodes = new TreeSet<NodeAddress>();
  // Nodes that have been banned by the source
  protected final Set<NodeAddress> sourceBannedNodes = new TreeSet<NodeAddress>();
  
  @Override public void nextCycle() {
    super.nextCycle();
    final Set<NodeAddress> nodesToDisconnectFrom = new TreeSet<NodeAddress>(alg.getNeighbors());
    nodesToDisconnectFrom.retainAll(blackListNodes);
    if (!nodesToDisconnectFrom.isEmpty()) {
      alg.DRB.removeNeighbors(nodesToDisconnectFrom);
      // TODO return this so I will notice when blacklist is working.
      // System.out.println("Disconnection in black list: " +
      // client.node.getImpl() + "," + nodesToDisconnectFrom
      // + "===========================================================");
    }
  }
  
  protected void addToBlackList(final NodeAddress node) {
    blackListNodes.add(node);
  }
  
  public static void addToBlackList(final NodeAddress blackListNode, final OverlayModule<?> overlay) {
    final BlackListIngredient blackList = (BlackListIngredient) overlay.getIngredient(BlackListIngredient.class);
    if (blackList != null) {
      blackList.addToBlackList(blackListNode);
    }
  }
  
  public static Set<NodeAddress> getSourceBannedNodes(final OverlayModule<?> overlay) {
    final BlackListIngredient blackList = (BlackListIngredient) overlay.getIngredient(BlackListIngredient.class);
    if (blackList != null) {
      return blackList.sourceBannedNodes;
    }
    return new TreeSet<NodeAddress>();
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof BlackListMessage) {
      assert false;
      final BlackListMessage blm = (BlackListMessage) message;
      assert blm.blamingNode.equals(client.network.getServerNode());
      if (sourceBannedNodes.contains(blm.blamedNode)) {
        return;
      }
      sourceBannedNodes.add(blm.blamedNode);
      blackListNodes.add(blm.blamedNode);
      final Set<NodeAddress> neighborsToInform = new TreeSet<NodeAddress>(alg.getNeighbors());
      neighborsToInform.remove(blm.sourceId);
      for (final NodeAddress neighbor : neighborsToInform) {
        client.network.send(new BlackListMessage(getMessageTag(), client.network.getAddress(), neighbor, blm.blamingNode,
            blm.blamedNode));
      }
    }
  }
}
