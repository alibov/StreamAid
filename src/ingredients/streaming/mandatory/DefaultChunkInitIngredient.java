package ingredients.streaming.mandatory;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.ChunkMessage;
import messages.Message;

public class DefaultChunkInitIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  public DefaultChunkInitIngredient(final Random r) {
    super(r);
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkMessage) {
      final ChunkMessage crm = (ChunkMessage) message;
      // for hop 1 nodes that don't receive bitmaps from the server
      if (client.player.getVs() == null) {
        client.player.initVS(crm.chunk.index);
      }
      client.player.getVs().updateChunk(crm.chunk);
    }
  }
}
