package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaConnectToMessage extends Message {
  /**
	 * 
	 */
  private static final long serialVersionUID = 6004285470322930138L;
  public NodeAddress h;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
  
  public AraneolaConnectToMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress h) {
    super(tag, sourceId, destID);
    this.h = h;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(h)) {
      h = newSource;
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return h.toString();
  }
}
