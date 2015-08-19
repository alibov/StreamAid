package ingredients.overlay.security;

import java.util.Random;

import modules.overlays.OverlayModule;
import experiment.frameworks.NodeAddress;

public class MalAuditingIngredient extends DNVPAuditingIngredient {
  public MalAuditingIngredient(final int thresholdForSource, final double thresholdForCommittee, final Random r) {
    super(thresholdForSource, thresholdForCommittee, r);
  }
  
  public static void logMisbehavior(final NodeAddress blamedNeighbor, final NodeAddress blamingNode,
      final OverlayModule<?> overlay, final Class<?> class1) {
    // Do nothing.
  }
}
