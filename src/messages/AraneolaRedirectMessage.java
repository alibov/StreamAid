package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaRedirectMessage extends Message {
  private static final long serialVersionUID = -7688671751469960162L;
  public NodeAddress neighbor;
  public final Integer degree;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE + NodeAddress.SIZE;
  }
  
  public AraneolaRedirectMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress neighbor, final Integer degree) {
    super(tag, sourceId, destID);
    this.neighbor = neighbor;
    this.degree = degree;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(neighbor)) {
      neighbor = newSource;
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return neighbor.toString();
  }
}
