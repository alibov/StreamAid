package messages;

import java.util.HashMap;
import java.util.Map;

import experiment.frameworks.NodeAddress;

public class FastMeshAcceptSendParentsMessage extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 645648964156L;
  public final Map<NodeAddress, Integer> parents;
  public NodeAddress[] _fatherChunkArr;
  public Map<NodeAddress, Long> _fatherchosenFathersDelays = new HashMap<NodeAddress, Long>();
  
  public FastMeshAcceptSendParentsMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Map<NodeAddress, Integer> bandwidthServedByFathers,
      final NodeAddress[] fatherChunkArr, final Map<NodeAddress, Long> fatherchosenFathersDelays) {
    super(tag, sourceId, destID);
    parents = bandwidthServedByFathers;
    _fatherChunkArr = fatherChunkArr;
    _fatherchosenFathersDelays = fatherchosenFathersDelays;
  }
  
  @Override protected String getContents() {
    // TODO Auto-generated method stub
    return "";
  }
  
  @Override public boolean isOverheadMessage() {
    // TODO Auto-generated method stub
    return true;
  }
}
