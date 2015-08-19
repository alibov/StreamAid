package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaConnectOKMessage extends Message {
  /**
	 * 
	 */
  private static final long serialVersionUID = -1065023987636132343L;
  public final int degree;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE;
  }
  
  public AraneolaConnectOKMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final int degree) {
    super(tag, sourceId, destID);
    this.degree = degree;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return degree + "";
  }
}
