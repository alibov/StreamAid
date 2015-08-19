package modules.streaming;

import java.util.Random;

import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.TreeOverlayModule;
import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class PushTree extends StreamingModule {
  public PushTree(final P2PClient client, final TreeOverlayModule<?> overlay, final Random r) {
    super(client, overlay, r);
  }
  
  public long lastReceivedChunkIndex = -1;
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ChunkMessage) {
      if (network.isServerMode()) {
        throw new RuntimeException("server received chunks message: " + message);
      }
      final ChunkMessage crm = (ChunkMessage) message;
      lastReceivedChunkIndex = crm.chunk.index;
      if (client.player.getVs() == null) {
        client.player.initVS(lastReceivedChunkIndex);
      }
      client.player.getVs().updateChunk(crm.chunk);
      if (crm.chunk.index < fromChunk || crm.chunk.index > latestChunk) {
        // return;
        System.out.println("sending chunk " + crm.chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
      }
      for (final NodeAddress neighbor : ((TreeOverlayModule<?>) mainOverlay).getSonNodes()) {
        network.send(new ChunkMessage(getMessageTag(), network.getAddress(), neighbor, crm.chunk));
      }
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (network.isServerMode()) {
      final VideoStreamChunk chunk = client.player.getVs().getLatestChunk();
      if (chunk == null) {
        return;
      }
      if (chunk.index < fromChunk || chunk.index > latestChunk) {
        return;
        // System.out.println("sending chunk " + chunk.index +
        // " out of protocol bounds: " + fromChunk + "-" + latestChunk);
      }
      for (final NodeAddress neighbor : mainOverlay.getNeighbors()) {
        network.send(new ChunkMessage(getMessageTag(), network.getAddress(), neighbor, chunk));
      }
    }
  }
  
  @Override public boolean isOverlayConnected() {
    return mainOverlay.isOverlayConnected() && client.player.getVs() != null && lastReceivedChunkIndex > -1
        && client.player.getVs().getFirstMissingChunk() > lastReceivedChunkIndex;
  }
}
