package messages.chunkySpread;

import java.util.Map;

import messages.Message;
import utils.chunkySpread.LatencyState;
import experiment.frameworks.NodeAddress;

public class NeighborLatencyChange extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 5945327156140345912L;
  private final Map<Integer, LatencyState> treesLatencies;
  
  public NeighborLatencyChange(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Map<Integer, LatencyState> _treesLatencies) {
    super(tag, sourceId, destID);
    treesLatencies = _treesLatencies;
  }
  
  @Override protected String getContents() {
    return treesLatencies.toString();
  }
  
  public Map<Integer, LatencyState> getTreesLatencies() {
    return treesLatencies;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
