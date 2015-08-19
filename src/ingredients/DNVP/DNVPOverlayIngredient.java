package ingredients.DNVP;

import ingredients.AbstractIngredient;
import ingredients.overlay.security.DNVPAuditingIngredient;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.DNVP.ApprovalRequest;
import messages.DNVP.ApprovalResponse;
import messages.DNVP.DisconnectionMessage;
import messages.DNVP.FirstVerificationResponse;
import messages.DNVP.IsNeighborRequest;
import messages.DNVP.IsNeighborResponse;
import messages.DNVP.IsNotNeighborResponse;
import messages.DNVP.IsNotRevokedResponse;
import messages.DNVP.IsRevokedRequest;
import messages.DNVP.IsRevokedResponse;
import messages.DNVP.LivenessMessage;
import messages.DNVP.NewNoncesMessage;
import messages.DNVP.NoncesDelivered;
import messages.DNVP.RemoveNeighborsNonces;
import messages.DNVP.VerificationRequest;
import messages.DNVP.VerificationResponse;
import modules.overlays.OverlayModule;
import utils.Common;
import utils.Utils;
import entites.DNVP.AuthorizationEntity;
import entites.DNVP.DNVPAuthorizationApproval;
import entites.DNVP.DNVPNonces;
import entites.DNVP.DNVPVerification;
import entites.DNVP.Nonce;
import experiment.frameworks.NodeAddress;

public class DNVPOverlayIngredient extends AbstractIngredient<OverlayModule<?>> {
  // =========================================Nonces============================================================================================
  // Neighbor to the expected nonces from him. The node generate this nonces and
  // sends them directly to his neighbor's neighbors.
  private final Map<NodeAddress, DNVPNonces> neighborToExpectedNoncesFrom = new TreeMap<NodeAddress, DNVPNonces>();
  // Neighbor to the nonces needs to be delivered to him. The node gets these
  // nonces and sends them to his neighbor as a proof for their connection.
  private final Map<NodeAddress, DNVPNonces> neighborToNoncesToDeliverTo = new TreeMap<NodeAddress, DNVPNonces>();
  // Neighbor to the nonces gathered for this neighbor. The nonces gathered from
  // this neighbor's neighbors and needs to be delivered to the specified
  // neighbor.
  private final Map<NodeAddress, DNVPNonces> neighborToNoncesGatheredFor = new TreeMap<NodeAddress, DNVPNonces>();
  // ========================================DNVP-output=========================================================================================
  // Neighbor to last verified neighbors.
  private final Map<NodeAddress, Set<NodeAddress>> neighborToItsNeighbors = new TreeMap<NodeAddress, Set<NodeAddress>>();
  // ========================================Past-events=========================================================================================
  // Neighbor To last verification from this neighbor.
  private final Map<NodeAddress, DNVPVerification> neighborToLastVerification = new TreeMap<NodeAddress, DNVPVerification>();
  // Neighbor to last round this neighbor was verified.
  private final Map<NodeAddress, Long> neighborToLastRoundVerified = new TreeMap<NodeAddress, Long>();
  // Neighbors at current cycle.
  private final Set<NodeAddress> neighborsAtCurrentCycle = new TreeSet<NodeAddress>();
  // Neighbors at previous cycle.
  private final Set<NodeAddress> neighborsAtPreviousCycle = new TreeSet<NodeAddress>();
  // Neighbors at current streaming cycle.
  private final Set<NodeAddress> neighborsAtCurrentStreamingCycle = new TreeSet<NodeAddress>();
  // Neighbors who sent me a liveness message in the last round.
  private final Set<NodeAddress> neighborsWhoSentMeLivenssMessage = new TreeSet<NodeAddress>();
  // Neighbors To expected Nonces for and from what round
  private final Map<NodeAddress/* neighbor who got nonces and needs to deliver
   * them */, Map<NodeAddress/* neighbor who sent
   * nonces for delivery */, Long/* round
   * these
   * nonces
   * were
   * sent */>> neighborToNoncesIssuedData = new TreeMap<NodeAddress, Map<NodeAddress, Long>>();
  // ========================================Pending-requests====================================================================================
  // Nodes that there are pending verification request to them, and the round
  // this request was sent.
  private final Map<NodeAddress, Long> sentVerificationRequests = new TreeMap<NodeAddress, Long>();
  // Pending Verification requests till the node gets his approval from source.
  private final Set<NodeAddress> pendingVerificationRequestsWaitingForApproval = new TreeSet<NodeAddress>();
  // ============================================================================================================================================
  // private int timeout = 0;
  private Long lastRound;
  private final int timeoutcycle;
  boolean everySecondCycle = false;
  // Number of nonces to produce for each neighbor's neighbor.
  private final int numOfNoncesToProduce;
  // Probability for checking whether an approval is revoked.
  private final double checkApproval;
  // Probability for checking whether a disconnection is real.
  private final double checkDisconnection;
  // Number of verification requests to issue each round.
  private final int verificationsPerRound;
  // Approval from source for this node's neighbors.
  public DNVPAuthorizationApproval authorizationApproval = null;
  // Is there an approval pending for reply at the source?
  private boolean pendingApprovalRequest = false;
  // ===============Authorization entity related fields=====================
  // The authorization entity.
  AuthorizationEntity authorizationEntity = null;
  // The Expiration interval for authorization entity approvals.
  private final int expirationInterval;
  
  // ============================================================================================================================================
  public DNVPOverlayIngredient(final int timeoutcycle, final int expirationInterval, final int numOfNoncesToProduce,
      final double checkApproval, final double checkDisconnection, final int verificationsPerRound, final Random r) {
    super(r);
    this.timeoutcycle = timeoutcycle;
    this.expirationInterval = expirationInterval;
    this.numOfNoncesToProduce = numOfNoncesToProduce;
    this.checkApproval = checkApproval;
    this.checkDisconnection = checkDisconnection;
    this.verificationsPerRound = verificationsPerRound;
    lastRound = getDNVPRound();
  }
  
  @Override public void setServerMode() {
    authorizationEntity = new AuthorizationEntity(client.network, getMessageTag(), expirationInterval, timeoutcycle);
  }
  
  public Set<NodeAddress> getNeighborNeighbors(final NodeAddress neighbor) {
    return neighborToItsNeighbors.get(neighbor);
  }
  
  public Map<NodeAddress, Set<NodeAddress>> getNeighborsNeighbors() {
    return neighborToItsNeighbors;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    updateFieldsEveryStreamingCycle();
    if (getDNVPRound() < lastRound + 1) {
      return;
    }
    TextLogger.log(client.network.getAddress(), "======================================= DNVP Cycle " + getDNVPRound()
        + " ====================================\n");
    lastRound = getDNVPRound();
    if (client.isServerMode()) {
      authorizationEntity.nextCycle();
    }
    updateFieldsEveryDNVPCycle();
    LivenessPhase();
    VerificationPhase();
  }
  
  private void updateFieldsEveryStreamingCycle() {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    TextLogger.log(client.network.getAddress(), "Neighbors are: " + currentNeighbors + " via " + this.getClass().getSimpleName()
        + "\n");
    final Set<NodeAddress> neighborsToRemove = new TreeSet<NodeAddress>();
    neighborsToRemove.addAll(neighborsAtCurrentStreamingCycle);
    neighborsToRemove.addAll(neighborToExpectedNoncesFrom.keySet());
    neighborsToRemove.addAll(neighborToNoncesGatheredFor.keySet());
    neighborsToRemove.addAll(neighborToNoncesToDeliverTo.keySet());
    neighborsToRemove.addAll(neighborToItsNeighbors.keySet());
    neighborsToRemove.addAll(neighborToLastVerification.keySet());
    neighborsToRemove.addAll(neighborToLastRoundVerified.keySet());
    neighborsToRemove.addAll(sentVerificationRequests.keySet());
    neighborsToRemove.addAll(pendingVerificationRequestsWaitingForApproval);
    neighborsToRemove.addAll(neighborsWhoSentMeLivenssMessage);
    neighborsToRemove.addAll(neighborToNoncesIssuedData.keySet());
    neighborsToRemove.removeAll(currentNeighbors);
    if (neighborsToRemove.size() > 0) {
      TextLogger.log(client.network.getAddress(), "Neighbors To remove: " + neighborsToRemove + " via "
          + this.getClass().getSimpleName() + "\n");
    }
    // Update all fields according to the new neighbors.
    neighborsAtCurrentStreamingCycle.clear();
    neighborsAtCurrentStreamingCycle.addAll(currentNeighbors);
    neighborToExpectedNoncesFrom.keySet().retainAll(currentNeighbors);
    neighborToNoncesGatheredFor.keySet().retainAll(currentNeighbors);
    neighborToNoncesToDeliverTo.keySet().retainAll(currentNeighbors);
    neighborToItsNeighbors.keySet().retainAll(currentNeighbors);
    neighborToLastVerification.keySet().retainAll(currentNeighbors);
    neighborToLastRoundVerified.keySet().retainAll(currentNeighbors);
    sentVerificationRequests.keySet().retainAll(currentNeighbors);
    pendingVerificationRequestsWaitingForApproval.retainAll(currentNeighbors);
    neighborsWhoSentMeLivenssMessage.retainAll(currentNeighbors);
    neighborToNoncesIssuedData.keySet().retainAll(currentNeighbors);
    if (neighborsToRemove.size() > 0) {
      for (final Map.Entry<NodeAddress, Map<NodeAddress, Long>> entry : neighborToNoncesIssuedData.entrySet()) {
        if (entry.getValue() != null) {
          entry.getValue().keySet().retainAll(currentNeighbors);
        }
      }
      for (final Map.Entry<NodeAddress, DNVPNonces> entry : neighborToNoncesGatheredFor.entrySet()) {
        entry.getValue().removeNoncesOfExNeighbors(currentNeighbors);
      }
      for (final NodeAddress neighbor : currentNeighbors) {
        client.network.send(new RemoveNeighborsNonces(getMessageTag(), client.network.getAddress(), neighbor, neighborsToRemove));
      }
    }
  }
  
  private void updateFieldsEveryDNVPCycle() {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    neighborsAtPreviousCycle.clear();
    neighborsAtPreviousCycle.addAll(neighborsAtCurrentCycle);
    neighborsAtCurrentCycle.clear();
    neighborsAtCurrentCycle.addAll(currentNeighbors);
    // Remove neighbors who didn't send me a liveness message for over a
    // DNVP-round and should have!
    if (everySecondCycle) {
      everySecondCycle = false;
      final Set<NodeAddress> nodes = new TreeSet<NodeAddress>();
      final Set<NodeAddress> neighborsToRemove = new TreeSet<NodeAddress>();
      nodes.addAll(neighborsAtPreviousCycle);
      nodes.retainAll(neighborsAtCurrentCycle);
      nodes.removeAll(neighborsWhoSentMeLivenssMessage);
      for (final NodeAddress neighbor : nodes) {
        if (neighborToNoncesIssuedData.get(neighbor) != null) {
          for (final Entry<NodeAddress, Long> entry : neighborToNoncesIssuedData.get(neighbor).entrySet()) {
            if (getDNVPRound() > entry.getValue() + 1) {
              neighborsToRemove.add(neighbor);
              break;
            }
          }
        }
      }
      for (final NodeAddress neighbor : neighborsToRemove) {
        // TODO think who should be the blaming node in here
        logMisbehavior(neighbor, client.network.getAddress());
      }
      neighborsWhoSentMeLivenssMessage.clear();
    } else {
      everySecondCycle = true;
    }
    // Remove neighbors with pending verification requests for too long.
    for (final Entry<NodeAddress, Long> entry : sentVerificationRequests.entrySet()) {
      if (entry.getValue() + 1 < getDNVPRound()) {
        logMisbehavior(entry.getKey(), client.network.getAddress());
      }
    }
    // Ask for a new approval if needed
    if ((authorizationApproval == null || authorizationApproval.expirationRound < (getDNVPRound() + 2) || !authorizationApproval.approvedNeighbors
        .containsAll(currentNeighbors)) && currentNeighbors.size() > 0 && !pendingApprovalRequest) {
      pendingApprovalRequest = true;
      client.network.send(new ApprovalRequest(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
          new TreeSet<NodeAddress>(currentNeighbors)));
    }
    // Send nonces to neighbors' neighbors if needed.
    for (final Map.Entry<NodeAddress, DNVPNonces> entry : neighborToExpectedNoncesFrom.entrySet()) {
      for (final NodeAddress neighborNeighbor : entry.getValue().getNeighborsToReproduce(getDNVPRound())) {
        entry.getValue().generateAndSendNonces(entry.getKey(), neighborNeighbor, getDNVPRound());
      }
    }
    // Answer pending requests if you can.
    if (authorizationApproval != null && authorizationApproval.approvedNeighbors.containsAll(currentNeighbors)) {
      for (final NodeAddress node : pendingVerificationRequestsWaitingForApproval) {
        sendResponseToVerificationRequest(node);
      }
      pendingVerificationRequestsWaitingForApproval.clear();
    }
  }
  
  private long getDNVPRound() {
    return (Utils.getMovieTime() / Common.currentConfiguration.cycleLength) / timeoutcycle;
  }
  
  protected void LivenessPhase() {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    for (final NodeAddress neighbor : neighborToNoncesToDeliverTo.keySet()) {
      if (currentNeighbors.contains(neighbor)) {
        final Map<NodeAddress, Nonce> noncesToSend = neighborToNoncesToDeliverTo.get(neighbor).getNextNonces();
        if (noncesToSend.size() > 0) {
          client.network.send(new LivenessMessage(getMessageTag(), client.network.getAddress(), neighbor, noncesToSend));
        }
      }
    }
  }
  
  private void VerificationPhase() {
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    for (final NodeAddress neighbor : currentNeighbors) {
      if (!neighborToLastRoundVerified.keySet().contains(neighbor) && !sentVerificationRequests.keySet().contains(neighbor)
          && neighborsAtPreviousCycle.contains(neighbor)) {
        sentVerificationRequests.put(neighbor, getDNVPRound());
        client.network.send(new VerificationRequest(getMessageTag(), client.network.getAddress(), neighbor));
      }
    }
    if (neighborToLastRoundVerified.size() > 0) {
      final Map<NodeAddress, Long> nodesToCheck = new TreeMap<NodeAddress, Long>();
      for (final Map.Entry<NodeAddress, Long> entry : neighborToLastRoundVerified.entrySet()) {
        if (entry.getValue() + 2 <= getDNVPRound() && !sentVerificationRequests.keySet().contains(entry.getKey())
            && currentNeighbors.contains(entry.getKey())) {
          nodesToCheck.put(entry.getKey(), entry.getValue());
        }
      }
      final Map<NodeAddress, Long> orderedMap = Utils.sortByValue(nodesToCheck, true);
      int i = 0;
      for (final Entry<NodeAddress, Long> entry : orderedMap.entrySet()) {
        sentVerificationRequests.put(entry.getKey(), getDNVPRound());
        client.network.send(new VerificationRequest(getMessageTag(), client.network.getAddress(), entry.getKey()));
        if (++i >= verificationsPerRound) {
          break;
        }
      }
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (client.isServerMode()) {
      authorizationEntity.handleMessage(message);
    }
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    if (message instanceof LivenessMessage) {
      if (!currentNeighbors.contains(message.sourceId)) {
        TextLogger.log(client.network.getAddress(), "Liveness message received from " + message.sourceId + " but ignored "
            + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      if (!checkLivenessMessage((LivenessMessage) message, neighborToNoncesIssuedData.get(message.sourceId))) {
        logMisbehavior(message.sourceId, client.network.getAddress());
        return;
      }
      neighborsWhoSentMeLivenssMessage.add(message.sourceId);
      final Map<NodeAddress, Nonce> noncesArrived = ((LivenessMessage) message).nonces;
      for (final NodeAddress neighbor : noncesArrived.keySet()) {
        if (currentNeighbors.contains(neighbor)) {
          if (neighborToNoncesGatheredFor.get(neighbor) == null) {
            neighborToNoncesGatheredFor.put(neighbor, new DNVPNonces(client, numOfNoncesToProduce, getMessageTag(), r));
          }
          neighborToNoncesGatheredFor.get(neighbor).addNonceForNeighbor(message.sourceId, noncesArrived.get(neighbor));
        }
      }
    }
    if (message instanceof VerificationRequest) {
      handleVerificationRequest((VerificationRequest) message);
    }
    if (message instanceof VerificationResponse) {
      if (!currentNeighbors.contains(message.sourceId)) {
        TextLogger.log(client.network.getAddress(), "Verification response received from " + message.sourceId
            + " but ignored cause they are not neighbors" + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      sentVerificationRequests.remove(message.sourceId);
      handleVerificationResponseMessage((VerificationResponse) message);
    }
    if (message instanceof FirstVerificationResponse) {
      if (!currentNeighbors.contains(message.sourceId)) {
        TextLogger.log(client.network.getAddress(), "First verification response received from " + message.sourceId
            + " but ignored " + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      // final DNVPVerification lastVerification =
      // neighborToLastVerification.get(message.sourceId);
      final DNVPVerification verification = ((FirstVerificationResponse) message).verification;
      // Check that this FirstVerificationMessage doesn't contain any nonces.
      if (verification.nonces.size() > 0) {
        logMisbehavior(message.sourceId, client.network.getAddress());
        return;
      }
      // Check that I'm not expecting any nonces from the neighbors that are in
      // this FirstVerification.
      if (neighborToExpectedNoncesFrom.get(message.sourceId) != null) {
        final Set<NodeAddress> neighborsIExpectNonceFrom = new TreeSet<NodeAddress>();
        neighborsIExpectNonceFrom.addAll(neighborToExpectedNoncesFrom.get(message.sourceId).getNeighbors());
        neighborsIExpectNonceFrom.retainAll(verification.claimedNeighbors);
        if (neighborsIExpectNonceFrom.size() > 0) {
          for (final NodeAddress neighborNeighbor : neighborsIExpectNonceFrom) {
            logMisbehavior(message.sourceId, neighborNeighbor);
          }
          return;
        }
      }
      // if (lastVerification != null && lastVerification.nonces.size() == 0) {
      // final Set<NodeSpecificImplementation> claimedNeighbors = new
      // TreeSet<NodeSpecificImplementation>();
      // claimedNeighbors.addAll(lastVerification.claimedNeighbors);
      // claimedNeighbors.retainAll(verification.claimedNeighbors);
      // claimedNeighbors.remove(client.node.getImpl());
      // if (claimedNeighbors.size() > 0) {
      // disconnectFromNeighbor(message.sourceId, null);
      // }
      // }
      sentVerificationRequests.remove(message.sourceId);
      neighborToLastRoundVerified.put(message.sourceId, getDNVPRound());
      neighborToLastVerification.put(message.sourceId, verification);
      final Set<NodeAddress> claimedNeighbors = verification.claimedNeighbors;
      if (isValidApproval(verification.approval)) {
        for (final NodeAddress node : claimedNeighbors) {
          if (!node.equals(client.network.getAddress())) {
            if (neighborToExpectedNoncesFrom.get(message.sourceId) == null) {
              neighborToExpectedNoncesFrom.put(message.sourceId, new DNVPNonces(client, numOfNoncesToProduce, getMessageTag(), r));
            }
            neighborToExpectedNoncesFrom.get(message.sourceId).generateAndSendNonces(message.sourceId, node, getDNVPRound());
          }
        }
      }
    }
    if (message instanceof NewNoncesMessage) {
      final NewNoncesMessage newNoncesMessage = ((NewNoncesMessage) message);
      if (!currentNeighbors.contains(newNoncesMessage.deliverToNeighbor)) {
        TextLogger.log(client.network.getAddress(), "New nonces message received from " + message.sourceId + " but ignored "
            + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      if (neighborToNoncesToDeliverTo.get(newNoncesMessage.deliverToNeighbor) == null) {
        neighborToNoncesToDeliverTo.put(newNoncesMessage.deliverToNeighbor, new DNVPNonces(client, numOfNoncesToProduce,
            getMessageTag(), r));
      }
      neighborToNoncesToDeliverTo.get(newNoncesMessage.deliverToNeighbor).addNoncesForNeighbor(message.sourceId,
          newNoncesMessage.nonces);
      neighborToNoncesToDeliverTo.get(newNoncesMessage.deliverToNeighbor).limitNoncesForNeighbor(message.sourceId, 1);
    }
    if (message instanceof NoncesDelivered) {
      if (!currentNeighbors.contains(message.sourceId)) {
        TextLogger.log(client.network.getAddress(), "NoncesDelivered message received from " + message.sourceId + " but ignored "
            + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      final NoncesDelivered noncesDelivered = (NoncesDelivered) message;
      if (neighborToNoncesIssuedData.get(noncesDelivered.neighborWhoGotNonces) == null) {
        neighborToNoncesIssuedData.put(noncesDelivered.neighborWhoGotNonces, new TreeMap<NodeAddress, Long>());
      }
      final Map<NodeAddress, Long> map = neighborToNoncesIssuedData.get(noncesDelivered.neighborWhoGotNonces);
      map.put(message.sourceId, noncesDelivered.roundNoncesIssued);
    }
    if (message instanceof RemoveNeighborsNonces) {
      if (!currentNeighbors.contains(message.sourceId)) {
        TextLogger.log(client.network.getAddress(), " RemoveNeighborsNonces received from " + message.sourceId + " but ignored "
            + " via " + this.getClass().getSimpleName() + "\n");
        return;
      }
      final Set<NodeAddress> neighborsToRemove = ((RemoveNeighborsNonces) message).nodesToRemove;
      TextLogger.log(client.network.getAddress(), " RemoveNeighborsNonces; for neighbor: " + message.sourceId
          + " removing neighbors: " + neighborsToRemove + " via " + this.getClass().getSimpleName() + "\n");
      if (neighborToExpectedNoncesFrom.get(message.sourceId) != null) {
        neighborToExpectedNoncesFrom.get(message.sourceId).removeNeighborsAndItsNonces(neighborsToRemove);
      }
      if (neighborToNoncesToDeliverTo.get(message.sourceId) != null) {
        neighborToNoncesToDeliverTo.get(message.sourceId).removeNeighborsAndItsNonces(neighborsToRemove);
      }
    }
    if (message instanceof ApprovalResponse) {
      if (!message.sourceId.equals(client.network.getServerNode())) {
        throw new IllegalStateException("Approval Response sent from node " + message.sourceId + " which is not the source: "
            + client.network.getServerNode());
      }
      pendingApprovalRequest = false;
      authorizationApproval = ((ApprovalResponse) message).approval;
      if (authorizationApproval != null && authorizationApproval.approvedNeighbors.containsAll(currentNeighbors)) {
        for (final NodeAddress node : pendingVerificationRequestsWaitingForApproval) {
          sendResponseToVerificationRequest(node);
        }
        pendingVerificationRequestsWaitingForApproval.clear();
      } else {
        // Ask for a new approval
        if (currentNeighbors.size() > 0) {
          pendingApprovalRequest = true;
          client.network.send(new ApprovalRequest(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
              new TreeSet<NodeAddress>(currentNeighbors)));
        }
      }
    }
    if (message instanceof IsRevokedResponse) {
      logMisbehavior(((IsRevokedResponse) message).approval.node, client.network.getAddress());
    }
    if (message instanceof IsNotRevokedResponse) {
      // Do nothing, everything is OK.
      throw new IllegalStateException("IsNotRevokedResponse Recieved");
    }
    if (message instanceof IsNeighborRequest) {
      final NodeAddress nodeToCheck = ((IsNeighborRequest) message).node;
      if (currentNeighbors.contains(nodeToCheck) && neighborsAtPreviousCycle.contains(nodeToCheck)
          && neighborsAtCurrentCycle.contains(nodeToCheck)) {
        client.network.send(new IsNeighborResponse(getMessageTag(), client.network.getAddress(), message.sourceId, nodeToCheck));
      } else {
        client.network.send(new IsNotNeighborResponse(getMessageTag(), client.network.getAddress(), message.sourceId, nodeToCheck));
      }
    }
    if (message instanceof IsNeighborResponse) {
      logMisbehavior(((IsNeighborResponse) message).node, message.sourceId);
    }
    if (message instanceof IsNotNeighborResponse) {
      // Do nothing, everything is OK.
    }
    // TODO check this.
    if (message instanceof DisconnectionMessage) {
      logMisbehavior(((DisconnectionMessage) message).problematicNode, client.network.getAddress());
      throw new IllegalStateException("No good!");
    }
  }
  
  private boolean checkLivenessMessage(final LivenessMessage message, final Map<NodeAddress, Long> map) {
    if (map == null) {
      return true;
    }
    for (final Entry<NodeAddress, Long> entry : map.entrySet()) {
      if (getDNVPRound() > entry.getValue() + 1) {
        if (!message.nonces.keySet().contains(entry.getKey())) {
          throw new IllegalStateException("Check LivenessMessage failed; should have delivered nonce from " + entry.getKey());
          // return false;
        }
      }
    }
    return true;
  }
  
  protected void handleVerificationRequest(final VerificationRequest message) {
    if (!alg.getNeighbors().contains(message.sourceId)) {
      TextLogger.log(client.network.getAddress(), "Verification request received from " + message.sourceId + " but ignored "
          + " via " + this.getClass().getSimpleName() + "\n");
      return;
    }
    if (authorizationApproval != null && authorizationApproval.approvedNeighbors.containsAll(alg.getNeighbors())) {
      sendResponseToVerificationRequest(message.sourceId);
    } else {
      TextLogger.log(client.network.getAddress(), "Verification request received from " + message.sourceId
          + " but wasn't answered due to approval " + " via " + this.getClass().getSimpleName() + "\n");
      pendingVerificationRequestsWaitingForApproval.add(message.sourceId);
    }
  }
  
  private void sendResponseToVerificationRequest(final NodeAddress node) {
    DNVPVerification verification;
    final Set<NodeAddress> currentNeighbors = alg.getNeighbors();
    if (neighborToNoncesGatheredFor.get(node) != null) {
      neighborToNoncesGatheredFor.get(node).removeNoncesOfExNeighbors(currentNeighbors);
      verification = new DNVPVerification(authorizationApproval, neighborToNoncesGatheredFor.get(node).neighborsNeighborToNonces,
          currentNeighbors);
      neighborToNoncesGatheredFor.remove(node);
      client.network.send(new VerificationResponse(getMessageTag(), client.network.getAddress(), node, verification));
    } else {
      verification = new DNVPVerification(authorizationApproval, new TreeMap<NodeAddress, LinkedList<Nonce>>(), currentNeighbors);
      client.network.send(new FirstVerificationResponse(getMessageTag(), client.network.getAddress(), node, verification));
    }
  }
  
  private boolean isValidApproval(final DNVPAuthorizationApproval approval) {
    if (approval.expirationRound < getDNVPRound() || !approval.approvedNeighbors.contains(client.network.getAddress())) {
      return false;
    }
    if (r.nextDouble() <= checkApproval) {
      client.network.send(new IsRevokedRequest(getMessageTag(), client.network.getAddress(), client.network.getServerNode(),
          approval));
    }
    return true;
  }
  
  private void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode) {
    // throw new IllegalStateException("Disconnection in legitimate mode: " +
    // client.node.getImpl() + "," + blamedNeighbor);
    // System.out.println("No good node " + blamedNeighbor);
    DNVPAuditingIngredient.logMisbehavior(blamedNeighbor, blamingNode, alg, getClass());
    // BlackListBehavior.addToBlackList(node, overlay);
  }
  
  private void handleVerificationResponseMessage(final VerificationResponse message) {
    final DNVPVerification verification = message.verification;
    // Check that all its claimed neighbors are inside the approval
    if (!verification.approval.approvedNeighbors.containsAll(verification.claimedNeighbors)) {
      logMisbehavior(message.sourceId, client.network.getAddress());
      return;
    }
    // Check nonces received.
    if (neighborToExpectedNoncesFrom.get(message.sourceId) == null || neighborToLastRoundVerified.get(message.sourceId) == null) {
      // throw new IllegalStateException("No expected nonces from " +
      // message.sourceId);
      logMisbehavior(message.sourceId, client.network.getAddress());
      return;
    }
    final Set<NodeAddress> neighborsToCheck = new TreeSet<NodeAddress>();
    final NodeAddress problematicNode = neighborToExpectedNoncesFrom.get(message.sourceId).checkVerificationMessageAndUpdate(
        verification.nonces, neighborsToCheck, neighborToLastRoundVerified.get(message.sourceId), getDNVPRound());
    if (problematicNode != null) {
      logMisbehavior(message.sourceId, problematicNode);
      return;
    }
    // Check approval from Authorization entity.
    if (!isValidApproval(verification.approval)) {
      logMisbehavior(message.sourceId, client.network.getAddress());
      return;
    }
    // Get new neighbors from verification to send nonces to. This part MUST(!)
    // be after the check of the nonces received.
    final Set<NodeAddress> newNeighborsToSendNoncesTo = new TreeSet<NodeAddress>();
    newNeighborsToSendNoncesTo.addAll(verification.claimedNeighbors);
    newNeighborsToSendNoncesTo.removeAll(neighborToExpectedNoncesFrom.get(message.sourceId).getNeighbors());
    newNeighborsToSendNoncesTo.removeAll(neighborsToCheck);
    for (final NodeAddress newNeighborNeighbor : newNeighborsToSendNoncesTo) {
      if (!newNeighborNeighbor.equals(client.network.getAddress())) {
        neighborToExpectedNoncesFrom.get(message.sourceId).generateAndSendNonces(message.sourceId, newNeighborNeighbor,
            getDNVPRound());
      }
    }
    // Ask neighbors that the node claims it has disconnected from, if it is
    // true.
    for (final NodeAddress neighbor : verification.approval.approvedNeighbors) {
      if (!client.network.getAddress().equals(neighbor) && !newNeighborsToSendNoncesTo.contains(neighbor)
          && !verification.nonces.keySet().contains(neighbor)) {
        if (r.nextDouble() <= checkDisconnection) {
          // TODO maybe add the nodes that I've sent requests to them to a
          // pending requests so I will know to accuse them when they don't
          // answer.
          client.network.send(new IsNeighborRequest(getMessageTag(), client.network.getAddress(), neighbor, message.sourceId));
        }
      }
    }
    final Set<NodeAddress> neighborNeighbors = new TreeSet<NodeAddress>();
    neighborNeighbors.addAll(verification.nonces.keySet());
    neighborNeighbors.add(client.network.getAddress());
    neighborToItsNeighbors.put(message.sourceId, neighborNeighbors);
    neighborToLastVerification.put(message.sourceId, verification);
    neighborToLastRoundVerified.put(message.sourceId, getDNVPRound());
    TextLogger.log(client.network.getAddress(), "Neighbors' neighbors are: " + neighborToItsNeighbors.toString() + " via "
        + this.getClass().getSimpleName() + "\n");
  }
}
