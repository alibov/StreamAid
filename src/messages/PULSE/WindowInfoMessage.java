package messages.PULSE;

import messages.Message;
import modules.overlays.PulseOverlay;
import modules.overlays.PulseOverlay.BufferRange;
import experiment.frameworks.NodeAddress;

public class WindowInfoMessage extends Message {
  public final BufferRange bufferRange;
  
  public WindowInfoMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final PulseOverlay.BufferRange bufferRange) {
    super(tag, sourceId, destID);
    this.bufferRange = bufferRange;
  }
  
  /**
   * 
   */
  private static final long serialVersionUID = 3758295637892L;
  
  @Override protected String getContents() {
    return bufferRange.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
