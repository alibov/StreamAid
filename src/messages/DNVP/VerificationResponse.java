package messages.DNVP;

import messages.Message;
import entites.DNVP.DNVPVerification;
import experiment.frameworks.NodeAddress;

public class VerificationResponse extends Message {
  private static final long serialVersionUID = -3898229706229403979L;
  public DNVPVerification verification;
  
  public VerificationResponse(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final DNVPVerification verification) {
    super(tag, sourceId, destID);
    this.verification = verification;
  }
  
  @Override protected String getContents() {
    return verification.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + verification.getSimulatedSize();
  }
}
