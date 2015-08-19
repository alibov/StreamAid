package messages.CoolStreamingPlus;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class PartnershipDropping extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public Integer substream;
  
  public PartnershipDropping(final Integer substream, final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
    this.substream = substream;
  }
  
  @Override protected String getContents() {
    return null;
  }
  
  @Override public boolean isOverheadMessage() {
    return false;
  }
}
