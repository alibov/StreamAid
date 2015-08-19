package messages;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public class PartialMembershipViewMessage<T extends Sizeable> extends Message {
  private static final long serialVersionUID = -2347963175782831369L;
  public final Map<NodeAddress, T> infoMap;
  
  @Override public long getSimulatedSize() {
    if (infoMap == null || infoMap.isEmpty()) {
      return super.getSimulatedSize();
    }
    long retVal = 0;
    for (final Sizeable t : infoMap.values()) {
      retVal += NodeAddress.SIZE;
      if (t != null) {
        retVal += t.getSimulatedSize();
      }
    }
    return retVal;
  }
  
  public PartialMembershipViewMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Map<NodeAddress, T> infoMap) {
    super(tag, sourceId, destID);
    this.infoMap = infoMap;
  }
  
  public PartialMembershipViewMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Collection<NodeAddress> info) {
    super(tag, sourceId, destID);
    infoMap = new TreeMap<NodeAddress, T>();
    for (final NodeAddress n : info) {
      infoMap.put(n, null);
    }
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return infoMap.toString();
  }
}
