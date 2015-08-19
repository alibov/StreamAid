package ingredients.streaming.optional;

import ingredients.AbstractIngredient;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.Message;
import modules.player.VideoStream;
import modules.streaming.StreamingModule;
import utils.Utils;
import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class SourcePushIngredient extends AbstractIngredient<StreamingModule> {
  public SourcePushIngredient(final Random r) {
    super(r);
  }
  
  public final Map<NodeAddress, Long> lastChunkSentMap = new TreeMap<NodeAddress, Long>();
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!client.isServerMode() || Utils.getMovieTime() < 0) {
      return;
    }
    final long offset = client.player.getVs().windowOffset;
    for (final NodeAddress nsi : alg.mainOverlay.getNeighbors()) {
      long lastChunkSent = Math.max(offset - 3, VideoStream.startingFrame);
      if (lastChunkSentMap.get(nsi) != null) {
        lastChunkSent = lastChunkSentMap.get(nsi) + 1;
      }
      lastChunkSent = Math.max(lastChunkSent, alg.fromChunk);
      final TreeSet<VideoStreamChunk> chunks = new TreeSet<VideoStreamChunk>();
      for (; lastChunkSent <= Math.min(offset, alg.latestChunk); lastChunkSent++) {
        VideoStreamChunk vsc = null;
        vsc = client.player.getVs().getChunk(lastChunkSent);
        if (vsc == null) {
          throw new RuntimeException("null content chunk!");
        }
        chunks.add(vsc);
      }
      if (chunks.isEmpty()) {
        continue;
      }
      for (final VideoStreamChunk chunk : chunks) {
        client.network.send(new ChunkMessage(alg.getMessageTag(), client.network.getAddress(), nsi, chunk));
      }
      lastChunkSentMap.put(nsi, offset);
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
