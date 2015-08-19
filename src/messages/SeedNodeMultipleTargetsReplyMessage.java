package messages;

import java.util.Collection;

import utils.Utils;
import experiment.frameworks.NodeAddress;

public class SeedNodeMultipleTargetsReplyMessage extends Message {
  private static final long serialVersionUID = -2249350186353613530L;
  public final Collection<NodeAddress> targets;
  public final long movieStartOffset;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE + NodeAddress.SIZE * targets.size();
  }
  
  public SeedNodeMultipleTargetsReplyMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Collection<NodeAddress> targets) {
    super(tag, sourceId, destID);
    this.targets = targets;
    movieStartOffset = Utils.getTime() - Utils.movieStartTime;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (targets.contains(sourceId)) {
      targets.remove(sourceId);
      targets.add(newSource);
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return targets.toString();
  }
}
