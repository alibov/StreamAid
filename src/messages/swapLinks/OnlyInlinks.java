package messages.swapLinks;

import experiment.frameworks.NodeAddress;

public class OnlyInlinks extends RandomWalk {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  public enum Algorithm {
    JOIN, CHURN
  }
  
  public final Algorithm algo;
  
  public OnlyInlinks(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress ori, final int rem, final Algorithm alg) {
    super(tag, sourceId, destID, ori, rem);
    algo = alg;
  }
  
  @Override protected String getContents() {
    return "alg: " + algo + ", " + super.getContents();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
