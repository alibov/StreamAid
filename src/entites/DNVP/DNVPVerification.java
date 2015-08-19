package entites.DNVP;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class DNVPVerification implements Sizeable {
  public DNVPAuthorizationApproval approval;
  // public DNVPNonces nonces;
  public Map<NodeAddress, LinkedList<Nonce>> nonces = new TreeMap<NodeAddress, LinkedList<Nonce>>();
  public Set<NodeAddress> claimedNeighbors = new TreeSet<NodeAddress>();
  
  public DNVPVerification(final DNVPAuthorizationApproval approval,
      final Map<NodeAddress, LinkedList<Nonce>> nonces, final Set<NodeAddress> claimedNeighbors) {
    this.approval = approval;
    this.nonces.putAll(nonces);
    this.claimedNeighbors.addAll(claimedNeighbors);
  }
  
  @Override public String toString() {
    return approval.toString() + " Claimed neighbors: " + claimedNeighbors.toString() + " DNVPNonces: " + nonces.toString();
  }
  
  @Override public long getSimulatedSize() {
    long noncesSize = 0;
    for (final java.util.Map.Entry<NodeAddress, LinkedList<Nonce>> entry : nonces.entrySet()) {
      noncesSize += NodeAddress.SIZE;
      noncesSize += Nonce.SIZE * entry.getValue().size();
    }
    return approval.getSimulatedSize() + noncesSize + NodeAddress.SIZE * claimedNeighbors.size();
  }
}
