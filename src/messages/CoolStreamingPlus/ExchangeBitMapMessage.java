package messages.CoolStreamingPlus;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class ExchangeBitMapMessage extends Message {
  public Long[] bitMap;
  
  public ExchangeBitMapMessage(final Long[] bitMap, final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
    this.bitMap = new Long[bitMap.length];
    for (int i = 0; i < bitMap.length; i++) {
      this.bitMap[i] = bitMap[i];
    }
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + bitMap.length;
  }
  
  private static final long serialVersionUID = -7555691347452453471L;
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    String str = new String();
    str += "[ ";
    for (final long l : bitMap) {
      str += l + ", ";
    }
    str += " ]";
    return str;
  }
}
