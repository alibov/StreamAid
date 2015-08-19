package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshGetMaxDelayAndRB extends Message {
  private static final long serialVersionUID = 856109823420027816L;
  
  /* public enum typeOfRequest { SEED_NODE_REQUEST, GRANT_REQUEST } public final
   * typeOfRequest requestType; */
  public FastMeshGetMaxDelayAndRB(final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
    // requestType = request;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  /* @Override protected String getContents() { if (requestType ==
   * typeOfRequest.SEED_NODE_REQUEST) { return "SEED_NODE_REQUEST"; } else if
   * (requestType == typeOfRequest.GRANT_REQUEST) { return "GRANT_REQUEST"; }
   * else { return "NO_SUCH_REQUEST"; } } */
  @Override protected String getContents() {
    return "";
  }
}
