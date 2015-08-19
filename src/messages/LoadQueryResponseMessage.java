package messages;

import experiment.frameworks.NodeAddress;

public class LoadQueryResponseMessage extends Message {
  private static final long serialVersionUID = -5425494456298035147L;
  public final long usedBandwidth;
  public final long uploadBandwidth;
  public final NodeAddress fatherNode;
  public final int distanceFromSource;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE + NodeAddress.SIZE + Long.SIZE * 2;
  }
  
  public LoadQueryResponseMessage(final String messageTag, final NodeAddress sourceId,
      final NodeAddress destID, final long usedBandwidth, final long uploadBandwidth, final int distanceFromSource,
      final NodeAddress fatherNode) {
    super(messageTag, sourceId, destID);
    this.usedBandwidth = usedBandwidth;
    this.uploadBandwidth = uploadBandwidth;
    this.fatherNode = fatherNode;
    this.distanceFromSource = distanceFromSource;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "used:" + usedBandwidth + "/" + uploadBandwidth;
  }
}
