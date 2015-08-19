package messages;

import experiment.frameworks.NodeAddress;

public class UploadBandwidthUpdateMessage extends Message {
  /**
	 * 
	 */
  private static final long serialVersionUID = 9079521958686877419L;
  public final long bandwidth;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE;
  }
  
  public UploadBandwidthUpdateMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final long bandwidth) {
    super(tag, sourceId, destID);
    this.bandwidth = bandwidth;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return String.valueOf(bandwidth);
  }
}
