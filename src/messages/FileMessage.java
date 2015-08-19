package messages;

import experiment.frameworks.NodeAddress;

public class FileMessage extends Message {
  private static final long serialVersionUID = -4080030807686430023L;
  public final byte[] bs;
  public final String zipName;
  
  public FileMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final String zipName, final byte[] bs) {
    super(tag, sourceId, destID);
    this.zipName = zipName;
    this.bs = bs;
  }
  
  @Override protected String getContents() {
    return zipName;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
