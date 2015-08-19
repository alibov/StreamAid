package messages;

import ingredients.overlay.InformationExchange.InfoType;

import java.util.Map;

import experiment.frameworks.NodeAddress;

public class InfoMessage extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 4335978695711048270L;
  public final Map<InfoType, Object> info;
  
  public InfoMessage(final String messageTag, final NodeAddress src, final NodeAddress dst, final Map<InfoType, Object> info) {
    super(messageTag, src, dst);
    this.info = info;
  }
  
  @Override protected String getContents() {
    return info.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + info.size() * (Long.SIZE * 2);
  }
}
