package ingredients.DNVP;

import java.util.Random;

import messages.DNVP.VerificationRequest;

public class Malicious2DNVPIngredinet extends DNVPOverlayIngredient {
  public Malicious2DNVPIngredinet(final int timeoutcycle, final int expirationInterval, final int numOfNoncesToProduce,
      final double checkApproval, final double checkDisconnection, final int verificationsPerRound, final Random r) {
    super(timeoutcycle, expirationInterval, numOfNoncesToProduce, checkApproval, checkDisconnection, verificationsPerRound, r);
  }
  
  @Override public String getMessageTag() {
    return alg.getMessageTag() + "-" + "DNVPOverlayBehavior";
  }
  
  @Override protected void handleVerificationRequest(final VerificationRequest message) {
    // Do nothing - this malicious node won't respond to verification requests.
  }
}
