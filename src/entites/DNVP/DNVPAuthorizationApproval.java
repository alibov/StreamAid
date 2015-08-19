package entites.DNVP;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

import java.util.Set;

public class DNVPAuthorizationApproval implements Sizeable {
  public NodeAddress node;
  public Set<NodeAddress> approvedNeighbors;
  public long expirationRound;
  
  public DNVPAuthorizationApproval(final NodeAddress node, final Set<NodeAddress> approvedNeighbors,
      final long expirationRound) {
    this.expirationRound = expirationRound;
    this.node = node;
    this.approvedNeighbors = approvedNeighbors;
  }
  
  @Override public long getSimulatedSize() {
    return (approvedNeighbors.size() + 1) * NodeAddress.SIZE + Long.SIZE;
  }
  
  @Override public String toString() {
    return "Approval{Node: " + node.toString() + " Approved Neighbors: " + approvedNeighbors.toString() + " Expiration Round: "
        + expirationRound + "}";
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return toString().equals(obj.toString());
  }
  
  @Override public int hashCode() {
    return toString().hashCode();
  }
}
