package ingredients.DNVP;

import java.util.Random;

public class Malicious1DNVPIngredient extends DNVPOverlayIngredient {
  public Malicious1DNVPIngredient(final int timeoutcycle, final int expirationInterval, final int numOfNoncesToProduce,
      final double checkApproval, final double checkDisconnection, final int verificationsPerRound, final Random r) {
    super(timeoutcycle, expirationInterval, numOfNoncesToProduce, checkApproval, checkDisconnection, verificationsPerRound, r);
  }
  
  @Override public String getMessageTag() {
    return alg.getMessageTag() + "-" + "DNVPOverlayBehavior";
  }
  
  @Override protected void LivenessPhase() {
    // Do nothing - this malicious node won't send liveness messages.
  }
}
