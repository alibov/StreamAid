package messages;

import experiment.frameworks.NodeAddress;

public class runMessage extends Message {
  public final String xmlFileName;
  
  public runMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final String xmlFileName) {
    super(tag, sourceId, destID);
    this.xmlFileName = xmlFileName;
  }
  
  private static final long serialVersionUID = -4637185800555618156L;
  
  @Override protected String getContents() {
    return xmlFileName;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
