package ingredients.streaming.mandatory;

import ingredients.AbstractIngredient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import messages.ChunkMessage;
import messages.ChunkRequestMessage;
import messages.Message;
import modules.streaming.StreamingModule;
import utils.Utils;
import entites.VideoStreamChunk;

public class HandleChunkRequestsSentAscendingIngredient extends AbstractIngredient<StreamingModule> {
  public HandleChunkRequestsSentAscendingIngredient(final Random r) {
    super(r);
  }
  
  private final Map<Long/* chunk */, Integer /* chunks sent */> chunksSent = new TreeMap<Long, Integer>();
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkRequestMessage) {
      final ChunkRequestMessage crm = (ChunkRequestMessage) message;
      final TreeMap<VideoStreamChunk, Integer> chunks = new TreeMap<VideoStreamChunk, Integer>();
      for (final Long reqChunk : crm.chunks) {
        final VideoStreamChunk vsc = client.player.getVs().getChunk(reqChunk);
        if (vsc == null) {
          // throw new RuntimeException("null content chunk (" + reqChunk +
          // ") requested by " + message.sourceId);
          // this probably happens when the chunk requested was already deleted
          continue;
        }
        Utils.checkExistence(chunksSent, reqChunk, 0);
        chunks.put(vsc, chunksSent.get(reqChunk));
      }
      // sending chunks using chunk share count
      final LinkedHashMap<VideoStreamChunk, Integer> sortedByShared = Utils.sortByValue(chunks, true);
      for (final VideoStreamChunk chunk : sortedByShared.keySet()) {
        if (chunk.index < alg.fromChunk || chunk.index > alg.latestChunk) {
          // continue;
          System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + alg.fromChunk + "-" + alg.latestChunk);
        }
        client.network.send(new ChunkMessage(alg.getMessageTag(), client.network.getAddress(), message.sourceId, chunk));
        chunksSent.put(chunk.index, chunksSent.get(chunk.index) + 1);
      }
    }
  }
}
