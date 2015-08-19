package ingredients.overlay.security;

import ingredients.AbstractIngredient;
import ingredients.DNVP.DNVPOverlayIngredient;
import ingredients.omission.OmissionDefenceIngredient;
import ingredients.omission.OmissionDefenceIngredient2;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
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

public class DNVPAuditingIngredient extends AbstractIngredient<OverlayModule<?>> {
  // =======================================Source-Fields===============================================
  // A map that the source holds for each of the nodes in the system.
  private Map<NodeAddress, Set<NodeAddress>> nodeToBlamingNodes = null;
  // A set of the black listed nodes that the source holds
  private Set<NodeAddress> blackListedNodes = null;
  // ===================================================================================================
  // A map that each of the nodes holds for each of their neighboring nodes.
  public final Map<NodeAddress, Set<NodeAddress>> neighborToAccusations = new TreeMap<NodeAddress, Set<NodeAddress>>();
  // Threshold for distributing a blacklist message by the source about one of
  // the system node.
  private final int thresholdForSource;
  // Threshold for disconnecting from one of your neighbors.
  private final double thresholdForCommittee;
  
  public DNVPAuditingIngredient(final int thresholdForSource, final double thresholdForCommittee, final Random r) {
    super(r);
    this.thresholdForSource = thresholdForSource;
    this.thresholdForCommittee = thresholdForCommittee;
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
      for (final Entry<NodeAddress, Set<NodeAddress>> entry : nodeToBlamingNodes.entrySet()) {
        entry.getValue().remove(message.blamedNode);
      }
      nodeToBlamingNodes.remove(message.blamedNode);
      // final int bannedNodeGroup =
      // Common.currentConfiguration.getNodeGroup(message.blamedNode.toString());
      // System.out.println("Black Listed: node-" + message.blamedNode +
      // ", group-" + bannedNodeGroup);
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
  
  private void handleAccusationMessage(final AccusationMessage message) {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    final DNVPAuthorizationApproval messageSourceApproval = message.approval;
    if (messageSourceApproval == null) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    final Set<NodeAddress> intersection = new TreeSet<NodeAddress>();
    intersection.addAll(messageSourceApproval.approvedNeighbors);
    intersection.retainAll(currentNeighbors);
    if (!(messageSourceApproval.approvedNeighbors.contains(client.network.getAddress())) && !(intersection.size() > 0)) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    if (!currentNeighbors.contains(message.blamedNode)) {
      throw new IllegalStateException("Attack on accusation mechanism");
    }
    logMisbehavior(message.blamedNode, message.sourceId, AccusationMessage.class);
  }
  
  public static void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode,
      final OverlayModule<?> overlay, final Class<?> class1) {
    final DNVPAuditingIngredient AUD = (DNVPAuditingIngredient) overlay.getIngredient(DNVPAuditingIngredient.class);
    if (AUD != null) {
      if (BlackListIngredient.getSourceBannedNodes(overlay).contains(blamedNeighbor)) {
        return;
      }
      if (!overlay.getNeighbors().contains(blamedNeighbor)) {
        throw new IllegalStateException("Blamed neighbor " + blamedNeighbor + " is not a neighbor of "
            + overlay.network.getAddress() + " which is the logging node.");
      }
      // Send a notification to both sides about the logging of this misbehavior
      AUD.sendAccusation(blamedNeighbor, blamingNode);
      AUD.sendAccusation(blamingNode, blamedNeighbor);
      // Log this misbehavior
      AUD.logMisbehavior(blamedNeighbor, blamingNode, class1);
      // Check that we didn't log a legitimate node
      final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blamedNeighbor.toString());
      if (blamedNodeGroup == 0) {
        // System.out.println("No Good============================================");
        throw new IllegalStateException("No Good============================================");
      }
    }
  }
  
  private void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode, final Class<?> class1) {
    printLogging(blamedNeighbor, blamingNode, class1);
    Utils.checkExistence(neighborToAccusations, blamedNeighbor, new TreeSet<NodeAddress>());
    neighborToAccusations.get(blamedNeighbor).add(blamingNode);
    if (blamingNode.equals(client.network.getAddress())
        || neighborToAccusations.get(blamedNeighbor).size() >= thresholdForCommittee) {
      disconnectFromNeighbor(blamedNeighbor);
    }
  }
  
  private void disconnectFromNeighbor(final NodeAddress neighborToDisconnectFrom) {
    // Print this disconnection
    // printDisconnection(neighborToDisconnectFrom);
    // Add myself to the accusing nodes
    // neighborToAccusations.get(neighborToDisconnectFrom).add(client.node.getImpl());
    // Send an accusation to the neighbor neighbors
    sendAccusationToNeighborNeighbors(neighborToDisconnectFrom);
    // Send a message to the source regarding this node
    client.network.send(new SourceAccusationMessage(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
        neighborToDisconnectFrom));
    // Add this node to local blacklist
    BlackListIngredient.addToBlackList(neighborToDisconnectFrom, alg);
    // Disconnect from this neighbor.
    alg.DRB.removeNeighbor(neighborToDisconnectFrom);
  }
  
  private void sendAccusationToNeighborNeighbors(final NodeAddress neighborToDisconnectFrom) {
    final DNVPOverlayIngredient DNVP = (DNVPOverlayIngredient) alg.getIngredient(DNVPOverlayIngredient.class);
    if (DNVP != null) {
      final Set<NodeAddress> neighborNeighborsToInform = DNVP.getNeighborNeighbors(neighborToDisconnectFrom);
      if (neighborNeighborsToInform != null) {
        for (final NodeAddress neighborNeighborToInform : neighborNeighborsToInform) {
          if (!neighborNeighborToInform.equals(client.network.getAddress())) {
            sendAccusation(neighborToDisconnectFrom, neighborNeighborToInform);
          }
        }
      }
    }
  }
  
  public static void sendAccusation(final NodeAddress blamedNode, final NodeAddress sendToNeighbor, final OverlayModule<?> overlay) {
    final DNVPAuditingIngredient AUD = (DNVPAuditingIngredient) overlay.getIngredient(DNVPAuditingIngredient.class);
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
    behaviors.add(DNVPOverlayIngredient.class);
    behaviors.add(OmissionDefenceIngredient.class);
    behaviors.add(AccusationMessage.class);
    behaviors.add(OmissionDefenceIngredient2.class);
  }
  
  // Printing disconnections for monitoring purposes.
  private void printLogging(final NodeAddress blamedNeighbor, final NodeAddress blamingNode, final Class<?> class1) {
    if (behaviors.contains(class1)) {
      final int myGroup = Common.currentConfiguration.getNodeGroup(client.network.getAddress().toString());
      final int blamedNodeGroup = Common.currentConfiguration.getNodeGroup(blamedNeighbor.toString());
      final int blamingNodeGroup = Common.currentConfiguration.getNodeGroup(blamingNode.toString());
      System.out.println("Log of misbehavior in " + class1.getSimpleName() + ": me-" + client.network.getAddress().toString() + ";"
          + myGroup + ", blamed-" + blamedNeighbor.toString() + ";" + blamedNodeGroup + ", blaming-" + blamingNode.toString() + ";"
          + blamingNodeGroup);
    }
  }
  // Printing disconnections for monitoring purposes.
  /* private void printDisconnection(final NodeAddress neighborToDisconnectFrom)
   * { System.out.println("Disconnection in " + getClass().getSimpleName() +
   * ": " + client.network.getAddress().toString() + ", " +
   * neighborToDisconnectFrom.toString()); } */
  // ======================================================================================================
}
