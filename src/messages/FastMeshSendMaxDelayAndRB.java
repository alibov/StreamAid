package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshSendMaxDelayAndRB extends Message {
  private static final long serialVersionUID = 6762147526948164757L;
  public final long maxDelay;
  public final int MRB;
  private boolean isReSend = false; // if we need to send the node updated
                                    // data.
  
  public enum typeOfRequest {
    SEED_NODE_REQUEST, GRANT_REQUEST
  }
  
  public FastMeshSendMaxDelayAndRB(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final long delay, final int RB) {
    super(tag, sourceId, destID);
    maxDelay = delay;
    MRB = RB;
  }
  
  // public final typeOfRequest requestType;
  public FastMeshSendMaxDelayAndRB(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final long delay, final int RB, final boolean isReSend) {
    this(tag, sourceId, destID, delay, RB);
    this.isReSend = isReSend;
    // requestType = request;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "maximun delay of node " + sourceId.toString() + " is: " + Long.toString(maxDelay) + " residual bandwidth :"
        + Integer.toString(MRB) + " did we sent the data again? " + isReSend;
  }
  
  public boolean getIsReSend() {
    return isReSend;
  }
}
