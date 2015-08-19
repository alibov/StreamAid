package messages.DNVP;

import messages.EmptyMessage;
import experiment.frameworks.NodeAddress;

public class VerificationRequest extends EmptyMessage {
  private static final long serialVersionUID = -2872143540859788860L;
  
  public VerificationRequest(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
