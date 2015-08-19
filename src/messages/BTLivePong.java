package messages;

import experiment.frameworks.NodeAddress;

public class BTLivePong extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 5506800059996053129L;
  public int upload;
  public int download;
  
  public BTLivePong(final String messageTag, final NodeAddress src, final NodeAddress dst, final int download, final int upload) {
    super(messageTag, src, dst);
    this.download = download;
    this.upload = upload;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + 2 * Integer.SIZE;
  }
  
  @Override protected String getContents() {
    return "d:" + download + " u:" + upload;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
