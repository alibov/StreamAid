package messages;

import java.util.Set;
import java.util.TreeSet;

import experiment.frameworks.NodeAddress;

public class ChunkRequestMessage extends Message {
  public final Set<Long> chunks = new TreeSet<Long>();
  
  @Override public long getSimulatedSize() {
    if (chunks == null) {
      return super.getSimulatedSize();
    }
    return super.getSimulatedSize() + Long.SIZE * chunks.size();
  }
  
  private static final long serialVersionUID = -4164403872061473258L;
  
  public ChunkRequestMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  public void addRequestedChunk(final long id) {
    chunks.add(id);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return chunks.toString();
  }
}
