package ingredients.network;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.ChunkMessage;
import messages.Message;

public class DropChunkMessages extends AbstractIngredient<NodeConnectionAlgorithm> {
  public final double dropRate;
  
  public DropChunkMessages(final double dropRate, final Random r) {
    super(r);
    this.dropRate = dropRate;
  }
  
  // pointcut chunkSend(Message m,NetworkNode node): args(m) && target(node) &&
  // call(boolean interfaces.NetworkNode.send(ChunkMessage));
  // boolean around(Message m,NetworkNode node): chunkSend(m,node) {
  @Override public void handleMessage(final Message message) {
    if (!(message instanceof ChunkMessage)) {
      return;
    }
    if (r.nextDouble() < dropRate) {
      client.network.flagMessage(message);
    }
  }
}
