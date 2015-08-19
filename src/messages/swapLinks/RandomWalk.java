package messages.swapLinks;

import messages.Message;
import experiment.frameworks.NodeAddress;

public abstract class RandomWalk extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public final NodeAddress origin;
  public final int remainingSteps;
  
  public RandomWalk(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress _origin, final int _remainingSteps) {
    super(tag, sourceId, destID);
    origin = _origin;
    remainingSteps = _remainingSteps;
  }
  
  @Override public long getSimulatedSize() {
    return 1; // TODO say what?!
  }
  
  @Override protected String getContents() {
    return "remaining walk length: " + remainingSteps + ", origin is " + origin;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
