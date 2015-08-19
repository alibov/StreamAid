package messages;

import java.util.Set;

import experiment.frameworks.NodeAddress;

public class FastMeshConnectionRequestApprovedMessage extends Message {
  private static final long serialVersionUID = 620221735804181342L;
  public final int bandwidthTakenFromFather;
  public Set<Integer> takenDescriptors;
  
  public FastMeshConnectionRequestApprovedMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final int bandwidthTaken, final Set<Integer> takenDescriptors) {
    super(tag, sourceId, destID);
    bandwidthTakenFromFather = bandwidthTaken;
    this.takenDescriptors = takenDescriptors;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "bandwidth taken from father: " + Integer.toString(bandwidthTakenFromFather);
  }
}
