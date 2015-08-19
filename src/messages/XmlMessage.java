package messages;

import experiment.frameworks.NodeAddress;

public class XmlMessage extends Message {
  private static final long serialVersionUID = 6372462763942450156L;
  public final String name;
  public final String contents;
  
  public XmlMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final String name, final String contents) {
    super(tag, sourceId, destID);
    this.name = name;
    this.contents = contents;
  }
  
  @Override protected String getContents() {
    return name;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
