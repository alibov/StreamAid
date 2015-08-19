package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaChangeConnectionMessage extends Message {
  /**
	 * 
	 */
  private static final long serialVersionUID = -1150804880226426085L;
  public final int degree;
  public NodeAddress n;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE + NodeAddress.SIZE;
  }
  
  public AraneolaChangeConnectionMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final int size, final NodeAddress n) {
    super(tag, sourceId, destID);
    degree = size;
    this.n = n;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(n)) {
      n = newSource;
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return degree + ", " + n.toString();
  }
}
