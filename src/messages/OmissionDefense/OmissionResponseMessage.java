package messages.OmissionDefense;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class OmissionResponseMessage extends Message {
  private static final long serialVersionUID = -445017724191358971L;
  public NodeAddress nodeToCheck;
  public Map<Long, Set<Long>> sentChunks = new TreeMap<Long, Set<Long>>();
  public Map<Long, Set<Long>> missingChunks = new TreeMap<Long, Set<Long>>();
  
  public OmissionResponseMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress nodeToCheck, final Map<Long, Set<Long>> sentChunks,
      final Map<Long, Set<Long>> missingChunks) {
    super(tag, sourceId, destID);
    this.nodeToCheck = nodeToCheck;
    if (sentChunks != null) {
      this.sentChunks.putAll(sentChunks);
    }
    if (missingChunks != null) {
      this.missingChunks.putAll(missingChunks);
    }
  }
  
  @Override protected String getContents() {
    return "Sent to node: " + nodeToCheck.toString() + " Sent chunks: " + sentChunks.toString() + " Missing Chunks: "
        + missingChunks.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    long sentChunksSize = 0;
    for (final java.util.Map.Entry<Long, Set<Long>> entry : sentChunks.entrySet()) {
      sentChunksSize += Long.SIZE;
      sentChunksSize += Long.SIZE * entry.getValue().size();
    }
    final long missingChunksSize = 0;
    for (final java.util.Map.Entry<Long, Set<Long>> entry : missingChunks.entrySet()) {
      sentChunksSize += Long.SIZE;
      sentChunksSize += Long.SIZE * entry.getValue().size();
    }
    return super.getSimulatedSize() + NodeAddress.SIZE + sentChunksSize + missingChunksSize;
  }
}
