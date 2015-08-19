package modules.streaming;

import java.util.Random;

import modules.P2PClient;
import modules.overlays.OverlayModule;

public class EmptyStreamingModule extends StreamingModule {
  public EmptyStreamingModule(final P2PClient client, final OverlayModule<?> overlay, final Random r) {
    super(client, overlay, r);
  }
}
