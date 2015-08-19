package messages;

import experiment.frameworks.NodeAddress;

public class ChunkReceivedMessage extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 123123548L;
  public long index;
  
  public ChunkReceivedMessage(final String messageTag, final NodeAddress src, final NodeAddress dst,
      final long index) {
    super(messageTag, src, dst);
    this.index = index;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE;
  }
  
  @Override protected String getContents() {
    return String.valueOf(index);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
