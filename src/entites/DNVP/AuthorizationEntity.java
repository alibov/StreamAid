package entites.DNVP;

import experiment.frameworks.NodeAddress;
import interfaces.AlgorithmComponent;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.Message;
import messages.DNVP.ApprovalRequest;
import messages.DNVP.ApprovalResponse;
import messages.DNVP.IsRevokedRequest;
import messages.DNVP.IsRevokedResponse;
import modules.network.NetworkModule;
import utils.Common;
import utils.Utils;

public class AuthorizationEntity implements AlgorithmComponent {
  // Subclass for revoked approvals.
  private class RevokedApproval {
    public DNVPAuthorizationApproval approval;
    public long roundRevoked;
    
    public RevokedApproval(final DNVPAuthorizationApproval approval, final long roundRevoked) {
      this.approval = approval;
      this.roundRevoked = roundRevoked;
    }
  }
  
  // Map between a node and its revoked approvals.
  private final Map<NodeAddress, LinkedList<RevokedApproval>> nodeToRevocationList = new TreeMap<NodeAddress, LinkedList<RevokedApproval>>();
  // Map between a node and its last issued approval.
  private final Map<NodeAddress, DNVPAuthorizationApproval> nodeToLastApproval = new TreeMap<NodeAddress, DNVPAuthorizationApproval>();
  // Map between a node and its number of requests made to the source
  private final Map<NodeAddress, Long> nodeToNumOfRequests = new TreeMap<NodeAddress, Long>();
  // Message tag.
  private final String messageTag;
  // Network node for sending messages.
  private final NetworkModule node;
  // Expiration interval
  private final int expirationInterval;
  // Timeout cycle
  final int timeoutcycle;
  
  public AuthorizationEntity(final NetworkModule node, final String messageTag, final int expirationInterval, final int timeoutcycle) {
    this.node = node;
    this.messageTag = messageTag;
    this.expirationInterval = expirationInterval;
    this.timeoutcycle = timeoutcycle;
  }
  
  private long getRound() {
    return (Utils.getMovieTime() / Common.currentConfiguration.cycleLength) / timeoutcycle;
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ApprovalRequest) {
      final DNVPAuthorizationApproval newApproval = new DNVPAuthorizationApproval(message.sourceId,
          ((ApprovalRequest) message).neighborsToApprove, getRound() + expirationInterval);
      final DNVPAuthorizationApproval oldApproval = nodeToLastApproval.get(message.sourceId);
      if (oldApproval != null && oldApproval.expirationRound >= getRound()) {
        if (nodeToRevocationList.get(message.sourceId) == null) {
          nodeToRevocationList.put(message.sourceId, new LinkedList<RevokedApproval>());
        }
        nodeToRevocationList.get(message.sourceId).add(new RevokedApproval(oldApproval, getRound()));
      }
      nodeToLastApproval.put(message.sourceId, newApproval);
      // Update the num of requests that this node made to the authorization
      // entity.
      if (nodeToNumOfRequests.get(message.sourceId) == null) {
        nodeToNumOfRequests.put(message.sourceId, Long.valueOf(0));
      }
      nodeToNumOfRequests.put(message.sourceId, nodeToNumOfRequests.get(message.sourceId) + 1);
      // Send a response to the approval request.
      node.send(new ApprovalResponse(messageTag, node.getAddress(), message.sourceId, newApproval));
    }
    if (message instanceof IsRevokedRequest) {
      final DNVPAuthorizationApproval approval = ((IsRevokedRequest) message).approval;
      if (nodeToRevocationList.get(approval.node) != null) {
        for (final RevokedApproval revokedApproval : nodeToRevocationList.get(approval.node)) {
          if (revokedApproval.approval.equals(approval) && revokedApproval.roundRevoked + 1 < getRound()) {
            node.send(new IsRevokedResponse(messageTag, node.getAddress(), message.sourceId, approval));
          }
        }
      }
      // node.send(new IsNotRevokedResponse(messageTag, node.getImpl(),
      // message.sourceId, approval));
    }
  }
  
  @Override public void nextCycle() {
    // Remove expired from revocation list.
    final Set<NodeAddress> nodesWithoutRevokedApprovals = new TreeSet<NodeAddress>();
    for (final Map.Entry<NodeAddress, LinkedList<RevokedApproval>> entry : nodeToRevocationList.entrySet()) {
      final LinkedList<RevokedApproval> expiredApprovals = new LinkedList<RevokedApproval>();
      for (final RevokedApproval approval : entry.getValue()) {
        if (approval.approval.expirationRound < getRound()) {
          expiredApprovals.add(approval);
        }
      }
      entry.getValue().removeAll(expiredApprovals);
      if (entry.getValue().size() <= 0) {
        nodesWithoutRevokedApprovals.add(entry.getKey());
      }
    }
    for (final NodeAddress nodeToRemove : nodesWithoutRevokedApprovals) {
      nodeToRevocationList.remove(nodeToRemove);
    }
    // Remove expired last approval.
    final Set<NodeAddress> nodesWithExpiredApproval = new TreeSet<NodeAddress>();
    for (final Map.Entry<NodeAddress, DNVPAuthorizationApproval> entry : nodeToLastApproval.entrySet()) {
      if (entry.getValue().expirationRound < getRound()) {
        nodesWithExpiredApproval.add(entry.getKey());
      }
    }
    for (final NodeAddress nodeToRemove : nodesWithExpiredApproval) {
      nodeToLastApproval.remove(nodeToRemove);
    }
  }
}
