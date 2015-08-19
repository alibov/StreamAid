package interfaces;

import modules.P2PClient;
import modules.overlays.OverlayModule;

public interface InfoGetter {
  Object getInfo(P2PClient client, OverlayModule<?> overlay);
}
