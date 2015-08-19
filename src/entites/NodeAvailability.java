package entites;

public class NodeAvailability {
  public NodeAvailability(final long joinTime, final long leaveTime) {
    this.joinTime = joinTime;
    this.leaveTime = leaveTime;
  }
  
  public long joinTime;
  public long leaveTime;
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (joinTime ^ (joinTime >>> 32));
    result = prime * result + (int) (leaveTime ^ (leaveTime >>> 32));
    return result;
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
    final NodeAvailability other = (NodeAvailability) obj;
    if (joinTime != other.joinTime) {
      return false;
    }
    if (leaveTime != other.leaveTime) {
      return false;
    }
    return true;
  }
}
