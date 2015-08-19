package messages.CoolStreamingPlus;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;
import messages.Message;

public class PartnershipRequestApprovedMessage<Payload extends Sizeable> extends Message {
  public PartnershipRequestApprovedMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  private static final long serialVersionUID = 3762143296221904686L;
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
