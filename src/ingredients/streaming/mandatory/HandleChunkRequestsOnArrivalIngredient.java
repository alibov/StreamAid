package ingredients.streaming.mandatory;

import ingredients.AbstractIngredient;

import java.util.Random;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.ChunkRequestMessage;
import messages.Message;
import modules.streaming.StreamingModule;
import entites.VideoStreamChunk;

public class HandleChunkRequestsOnArrivalIngredient extends AbstractIngredient<StreamingModule> {
  public HandleChunkRequestsOnArrivalIngredient(final Random r) {
    super(r);
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkRequestMessage) {
      if (!client.network.isUp(message.sourceId)) {
        return;
      }
      final ChunkRequestMessage crm = (ChunkRequestMessage) message;
      final TreeSet<VideoStreamChunk> chunks = new TreeSet<VideoStreamChunk>();
      for (final Long reqChunk : crm.chunks) {
        final VideoStreamChunk vsc = client.player.getVs().getChunk(reqChunk);
        if (vsc == null) {
          // throw new RuntimeException("null content chunk (" + reqChunk +
          // ") requested by " + message.sourceId);
          // this probably happens when the chunk requested was already deleted
          continue;
        }
        chunks.add(vsc);
      }
      for (final VideoStreamChunk chunk : chunks) {
        if (chunk.index < alg.fromChunk || chunk.index > alg.latestChunk) {
          /* System.out.println("got chunk " + chunk.index +
           * " request out of protocol bounds: " + alg.fromChunk + "-" +
           * alg.latestChunk); */
          // continue;
        }
        client.network.send(new ChunkMessage(alg.getMessageTag(), client.network.getAddress(), message.sourceId, chunk));
      }
    }
  }
}
