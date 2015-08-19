package ingredients.overlay.security;

import ingredients.AbstractIngredient;
import ingredients.DNVP.DNVPOverlayIngredient;
import ingredients.omission.OmissionDefenceIngredient2;

import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.Message;
import messages.OmissionDefense.AccusationMessage;
import messages.OmissionDefense.SourceAccusationMessage;
import modules.network.peersim.SessionEndMessage;
import modules.overlays.OverlayModule;
import utils.Common;
import utils.Utils;
import entites.DNVP.DNVPAuthorizationApproval;
import experiment.frameworks.NodeAddress;

public class FreeridingAuditingIngredient extends AbstractIngredient<OverlayModule<?>> {
  // =======================================Source-Fields===============================================
  // A map that the source holds for each of the nodes in the system.
  private Map<NodeAddress, Set<NodeAddress>> nodeToBlamingNodes = null;
  // A set of the black listed nodes that the source holds
  private Set<NodeAddress> blackListedNodes = null;
  // ===================================================================================================
  // Threshold for distributing a blacklist message by the source about one of
  // the system nodes.
  private final int thresholdForSource;
  
  public FreeridingAuditingIngredient(final int thresholdForSource, final Random r) {
    super(r);
    this.thresholdForSource = thresholdForSource;
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    nodeToBlamingNodes = new TreeMap<NodeAddress, Set<NodeAddress>>();
    blackListedNodes = new TreeSet<NodeAddress>();
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof AccusationMessage) {
      handleAccusationMessage((AccusationMessage) message);
    }
    if (message instanceof SourceAccusationMessage) {
      assert client.isServerMode();
      handleSourceAccusationMessage((SourceAccusationMessage) message);
    }
  }
  
  private void handleAccusationMessage(final AccusationMessage message) {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    final DNVPAuthorizationApproval messageSourceApproval = message.approval;
    if (messageSourceApproval == null) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    // Check that the accusation is from one of my neighbors or my neighbors'
    // neighbors.
    final Set<NodeAddress> intersection = new TreeSet<NodeAddress>();
    intersection.addAll(messageSourceApproval.approvedNeighbors);
    intersection.retainAll(currentNeighbors);
    if (!(messageSourceApproval.approvedNeighbors.contains(client.network.getAddress())) && !(intersection.size() > 0)) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    // Check that the accusation is against one of my neighbors
    if (!currentNeighbors.contains(message.blamedNode)) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    logMisbehavior(message.blamedNode, message.sourceId, AccusationMessage.class);
  }
  
  private void handleSourceAccusationMessage(final SourceAccusationMessage message) {
    if (blackListedNodes.contains(message.blamedNode)) {
      return;
    }
    Utils.checkExistence(nodeToBlamingNodes, message.blamedNode, new TreeSet<NodeAddress>());
    nodeToBlamingNodes.get(message.blamedNode).add(message.sourceId);
    // Check if we need to ban this node from the system.
    if (nodeToBlamingNodes.get(message.blamedNode).size() >= thresholdForSource) {
      blackListedNodes.add(message.blamedNode);
      client.network.send(new SessionEndMessage(message.blamedNode));
      nodeToBlamingNodes.remove(message.blamedNode);
      final int bannedNodeGroup = Common.currentConfiguration.getNodeGroup(message.blamedNode.toString());
      System.err.println("Black Listed: node-" + message.blamedNode + ", group-" + bannedNodeGroup);
      // for (final NodeSpecificImplementation neighbor :
      // overlay.getNeighbors()) {
      // client.node.send(new BlackListMessage(overlay.getMessageTag(),
      // client.node.getImpl(), neighbor,
      // client.node.getServerNode(), message.blamedNode));
      // }
      // if (bannedNodeGroup == 0) {
      // System.out.println("Blaming nodes: " +
      // nodeToBlamingNodes.get(message.blamedNode));
      // throw new
      // IllegalStateException("Legitimate node banned from the system");
      // }
    }
  }
  
  public static void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode,
      final OverlayModule<?> overlay, final Class<?> class1) {
    final FreeridingAuditingIngredient AUD = (FreeridingAuditingIngredient) overlay
        .getIngredient(FreeridingAuditingIngredient.class);
    if (AUD != null) {
      if (BlackListIngredient.getSourceBannedNodes(overlay).contains(blamedNeighbor)) {
        return;
      }
      if (!overlay.getNeighbors().contains(blamedNeighbor)) {
        throw new IllegalStateException("Blamed neighbor " + blamedNeighbor + " is not a neighbor of "
            + overlay.network.getAddress() + " which is the logging node.");
      }
      // Log this misbehavior
      AUD.logMisbehavior(blamedNeighbor, blamingNode, class1);
    }
  }
  
  private void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode, final Class<?> class1) {
    printLogging(blamedNeighbor, blamingNode, class1);
    // Check that we didn't log a legitimate node
    final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blamedNeighbor.toString());
    if (blamedNodeGroup == 0) {
      // System.err.println("Logged misbehavior of a legitimate node");
      throw new IllegalStateException("Logged misbehavior of a legitimate node");
    }
    disconnectFromNeighbor(blamedNeighbor);
  }
  
  private void disconnectFromNeighbor(final NodeAddress neighborToDisconnectFrom) {
    // Print this disconnection
    // printDisconnection(neighborToDisconnectFrom);
    // Send an accusation to the neighbor neighbors
    // sendAccusationToNeighborNeighbors(neighborToDisconnectFrom);
    // Send a message to the source regarding this node
    client.network.send(new SourceAccusationMessage(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
        neighborToDisconnectFrom));
    // Add this node to local blacklist
    BlackListIngredient.addToBlackList(neighborToDisconnectFrom, alg);
    // Disconnect from this neighbor.
    alg.DRB.removeNeighbor(neighborToDisconnectFrom);
  }
  
  // private void sendAccusationToNeighborNeighbors(final
  // NodeSpecificImplementation neighborToDisconnectFrom) {
  // final DNVPOverlayBehavior DNVP = (DNVPOverlayBehavior)
  // overlay.getBehavior(DNVPOverlayBehavior.class);
  // if (DNVP != null) {
  // final Set<NodeSpecificImplementation> neighborNeighborsToInform =
  // DNVP.getNeighborNeighbors(neighborToDisconnectFrom);
  // if (neighborNeighborsToInform != null) {
  // for (final NodeSpecificImplementation neighborNeighborToInform :
  // neighborNeighborsToInform) {
  // if (!neighborNeighborToInform.equals(client.node.getImpl())) {
  // sendAccusation(neighborToDisconnectFrom, neighborNeighborToInform);
  // }
  // }
  // }
  // }
  // }
  public static void sendAccusation(final NodeAddress blamedNode, final NodeAddress sendToNeighbor, final OverlayModule<?> overlay) {
    final FreeridingAuditingIngredient AUD = (FreeridingAuditingIngredient) overlay
        .getIngredient(FreeridingAuditingIngredient.class);
    if (AUD != null) {
      AUD.sendAccusation(blamedNode, sendToNeighbor);
    }
  }
  
  private void sendAccusation(final NodeAddress blamedNode, final NodeAddress sendToNeighbor) {
    // If the blamed node is me, I won't send this accusation because it's
    // against my interests.
    if (blamedNode.equals(client.network.getAddress())) {
      return;
    }
    final DNVPOverlayIngredient DNVP = (DNVPOverlayIngredient) alg.getIngredient(DNVPOverlayIngredient.class);
    if (DNVP != null) {
      final DNVPAuthorizationApproval approval = DNVP.authorizationApproval;
      if (approval != null) {
        client.network.send(new AccusationMessage(getMessageTag(), client.network.getAddress(), sendToNeighbor, blamedNode,
            approval));
      }
    }
  }
  
  // ==========================================Printing-section===========================================
  // Behaviors that I want to see their logging.
  private static final LinkedList<Class<?>> behaviors = new LinkedList<Class<?>>();
  static {
    behaviors.add(AccusationMessage.class);
    behaviors.add(OmissionDefenceIngredient2.class);
  }
  
  // Printing disconnections for monitoring purposes.
  private void printLogging(final NodeAddress blamedNeighbor, final NodeAddress blamingNode, final Class<?> class1) {
    if (behaviors.contains(class1)) {
      final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
      final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blamedNeighbor.toString());
      final int blamingNodeGroup = Common.currentConfiguration.getNodeGroup(blamingNode.toString());
      if (blamedNodeGroup == 1) {
        return;
      }
      System.out.println(this.getClass().getSimpleName() + ": Log of misbehavior in " + class1.getSimpleName() + ": me-"
          + client.network.getAddress().toString() + ";" + myGroup + ", blamed-" + blamedNeighbor.toString() + ";"
          + blamedNodeGroup + ", blaming-" + blamingNode.toString() + ";" + blamingNodeGroup);
    }
  }
  // Printing disconnections for monitoring purposes.
  /* private void printDisconnection(final NodeAddress neighborToDisconnectFrom)
   * { System.out.println("Disconnection in " + getClass().getSimpleName() +
   * ": " + client.network.getAddress().toString() + ", " +
   * neighborToDisconnectFrom.toString()); } */
  // ======================================================================================================
}
