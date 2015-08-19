package messages.CoolStreamingPlus;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class PartnershipRequestApprovedMessageaAck extends Message {
  public PartnershipRequestApprovedMessageaAck(final String tag, final NodeAddress sourceId,
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
