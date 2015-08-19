package messages.OmissionDefense;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class MalWhatToAnswerRequest extends Message {
  private static final long serialVersionUID = -7288390992377494843L;
  public NodeAddress answerToNode;
  
  public MalWhatToAnswerRequest(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress answerToNode) {
    super(tag, sourceId, destID);
    this.answerToNode = answerToNode;
  }
  
  @Override protected String getContents() {
    return answerToNode.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
}
