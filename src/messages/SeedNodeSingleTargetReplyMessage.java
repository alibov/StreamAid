package messages;

import utils.Utils;
import experiment.frameworks.NodeAddress;

public class SeedNodeSingleTargetReplyMessage extends Message {
  private static final long serialVersionUID = 7374002732956613421L;
  public NodeAddress target;
  public final long movieStartOffset;

  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE + NodeAddress.SIZE;
  }

  public SeedNodeSingleTargetReplyMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress target) {
    super(tag, sourceId, destID);
    this.target = target;
    movieStartOffset = Utils.getTime() - Utils.movieStartTime;
  }

  @Override public boolean isOverheadMessage() {
    return true;
  }

  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(target)) {
      target = newSource;
    }
    super.updateSourceID(newSource);
  }

  @Override protected String getContents() {
    return target.toString() + " offset: " + movieStartOffset;
  }
}
