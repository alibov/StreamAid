package modules.streaming;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.MultipleTreeOverlayModule;
import utils.Utils;
import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class PushMultipleDescriptionTree extends StreamingModule {
  public PushMultipleDescriptionTree(final P2PClient client, final MultipleTreeOverlayModule<?> overlay, final Random r) {
    super(client, overlay, r);
  }
  
  long lastReceivedChunkIndex = 0;
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ChunkMessage) {
      final VideoStreamChunk chunk = ((ChunkMessage) message).chunk;
      lastReceivedChunkIndex = chunk.index;
      if (client.player.getVs() == null) {
        client.player.initVS(lastReceivedChunkIndex);
      }
      client.player.getVs().updateChunk(chunk);
      if (chunk.index < fromChunk || chunk.index > latestChunk) {
        // return;
        System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
      }
      disseminateChunkToSons(chunk);
    }
  }
  
  private void disseminateChunkToSons(final VideoStreamChunk chunk) {
    final Map<NodeAddress, TreeSet<Integer>> sonToChunks = new TreeMap<NodeAddress, TreeSet<Integer>>();
    for (final int i : chunk.getDescriptions()) {
      for (final NodeAddress son : ((MultipleTreeOverlayModule<?>) mainOverlay).getSonNodes(i)) {
        Utils.checkExistence(sonToChunks, son, new TreeSet<Integer>());
        sonToChunks.get(son).add(i);
      }
    }
    for (final NodeAddress son : sonToChunks.keySet()) {
      network.send(new ChunkMessage(getMessageTag(), network.getAddress(), son, chunk.getMDChunk(sonToChunks.get(son))));
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!network.isServerMode()) {
      return;
    }
    final VideoStreamChunk chunk = client.player.getVs().getLatestChunk();
    if (chunk == null) {
      return;
    }
    if (chunk.index < fromChunk || chunk.index > latestChunk) {
      // return;
      System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
    }
    disseminateChunkToSons(chunk);
  }
  
  @Override public boolean isOverlayConnected() {
    return mainOverlay.isOverlayConnected() && client.player.getVs() != null
        && client.player.getVs().getFirstMissingChunk() > lastReceivedChunkIndex;
  }
}
