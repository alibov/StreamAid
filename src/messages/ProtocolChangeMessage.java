package messages;

import org.w3c.dom.Node;

import experiment.XMLutils;
import experiment.frameworks.NodeAddress;

public class ProtocolChangeMessage extends Message {
  public final long fromChunk;
  public final int newConfNumber;
  public final Node node;
  
  public ProtocolChangeMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int newConfNumber,
      final Node node, final long fromChunk) {
    super(tag, sourceId, destID);
    this.newConfNumber = newConfNumber;
    this.node = node;
    this.fromChunk = fromChunk;
  }
  
  private static final long serialVersionUID = -5020927028872635651L;
  
  @Override protected String getContents() {
    return "from " + fromChunk + " conf " + newConfNumber + " node: " + node;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE + Integer.SIZE + XMLutils.nodeToString(node).length() * Character.SIZE;
  }
}
