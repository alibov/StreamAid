package logging.logObjects;

public class SendLog extends DataLog {
  private static final long serialVersionUID = 7767258800600596939L;
  public final long messageSize;
  public final String receiveingNode;
  public final boolean isOverhead;
  public final String messageTag;
  public final String messageType;
  
  public SendLog(final String messageTag, final String messageType, final long messageSize, final String sendingNode,
      final String receiveingNode, final boolean isOverhead) {
    super(sendingNode);
    this.messageSize = messageSize;
    this.receiveingNode = receiveingNode;
    this.messageTag = messageTag;
    this.isOverhead = isOverhead;
    this.messageType = messageType;
  }
}
