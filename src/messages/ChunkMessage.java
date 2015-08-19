package messages;

import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class ChunkMessage extends Message {
  private static final long serialVersionUID = -6981216648795525345L;
  public final VideoStreamChunk chunk;

  @Override public long getSimulatedSize() {
    if (chunk == null) {
      return super.getSimulatedSize();
    }
    return super.getSimulatedSize() + chunk.getSimulatedSize();
  }

  public ChunkMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID, final VideoStreamChunk chunk) {
    super(tag, sourceId, destID);
    this.chunk = chunk;
  }

  @Override public boolean isOverheadMessage() {
    return false;
  }

  @Override protected String getContents() {
    return chunk.toString();
  }
}
