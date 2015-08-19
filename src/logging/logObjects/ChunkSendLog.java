package logging.logObjects;

public class ChunkSendLog extends SendLog {
  /**
   * 
   */
  private static final long serialVersionUID = 36895623L;
  public final long chunkId;
  
  public ChunkSendLog(final String messageTag, final String messageType, final long messageSize, final String sendingNode,
      final String receiveingNode, final boolean isOverhead, final long chunkId) {
    super(messageTag, messageType, messageSize, sendingNode, receiveingNode, isOverhead);
    this.chunkId = chunkId;
  }
}
