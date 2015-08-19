package modules.streaming;

import ingredients.overlay.NeighborChunkUnavailabilityIngredient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import logging.TextLogger;
import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.MultipleTreeOverlayModule;
import utils.Common;
import utils.Utils;
import entites.Availability;
import entites.Bitmap;
import entites.VideoStreamChunk;
import experiment.frameworks.NodeAddress;

public class PushMultipleSubstreamTree extends StreamingModule {
  MultipleTreeOverlayModule<?> overlay;
  Map<Integer, NeighborChunkUnavailabilityIngredient> sonChunkUnavailability = new HashMap<Integer, NeighborChunkUnavailabilityIngredient>();
  private final boolean sendMissingChunks;
  
  public PushMultipleSubstreamTree(final P2PClient client, final MultipleTreeOverlayModule<?> overlay,
      final boolean sendMissingChunks, final Random r) {
    super(client, overlay, r);
    this.overlay = overlay;
    this.sendMissingChunks = sendMissingChunks;
    for (int i = 0; i < Common.currentConfiguration.descriptions; ++i) {
      final NeighborChunkUnavailabilityIngredient NCUB = new NeighborChunkUnavailabilityIngredient(i, new Random(r.nextLong()));
      sonChunkUnavailability.put(i, NCUB);
      overlay.addIngredient(NCUB, client);
    }
  }
  
  long lastReceivedChunkIndex = 0;
  Set<Long> receivedChunks = new HashSet<Long>();
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ChunkMessage) {
      final VideoStreamChunk chunk = ((ChunkMessage) message).chunk;
      if (client.player.getVs() == null) {
        client.player.initVS(chunk.index);
      }
      // if it's a new chunk (for dag overlays)
      if (!receivedChunks.contains(chunk.index)) {
        receivedChunks.add(chunk.index);
        if (chunk.index < fromChunk || chunk.index > latestChunk) {
          // return;
          System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
        }
        disseminateChunkToSons(chunk);
      }
    }
  }
  
  private void disseminateChunkToSons(final VideoStreamChunk chunk) {
    lastReceivedChunkIndex = chunk.index;
    final int club = (int) (chunk.index % Common.currentConfiguration.descriptions);
    for (final NodeAddress son : ((MultipleTreeOverlayModule<?>) mainOverlay).getSonNodes(club)) {
      if (!sonChunkUnavailability.get(club).sonAvailability.containsKey(son)
          || !sonChunkUnavailability.get(club).sonAvailability.get(son).hasChunk(chunk.index)) {
        network.send(new ChunkMessage(getMessageTag(), network.getAddress(), son, chunk));
        Utils.checkExistence(sonChunkUnavailability.get(club).sonAvailability, son, new Availability(new Bitmap(chunk.index)));
        sonChunkUnavailability.get(club).sonAvailability.get(son).updateChunk(chunk.index);
      } else {
        TextLogger.log(network.getAddress(), "saved chunk duplication! didn't send to " + son.toString() + "\n");
      }
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (sendMissingChunks) {
      sendMissingChunksToSons();
    }
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
  
  private void sendMissingChunksToSons() {
    for (int club = 0; club < Common.currentConfiguration.descriptions; ++club) {
      for (final NodeAddress son : ((MultipleTreeOverlayModule<?>) mainOverlay).getSonNodes(club)) {
        if (!sonChunkUnavailability.get(club).sonAvailability.containsKey(son)
            || sonChunkUnavailability.get(club).sonAvailability.get(son).isEmpty()) {
          continue;
        }
        final Availability avail = sonChunkUnavailability.get(club).sonAvailability.get(son);
        long first = avail.getFirstMissingChunk() / Common.currentConfiguration.descriptions
            * Common.currentConfiguration.descriptions + club;
        for (; first < lastReceivedChunkIndex; first += Common.currentConfiguration.descriptions) {
          if (!avail.hasChunk(first) && avail.getEarliestChunkSeen() < first) {
            final VideoStreamChunk chunk = client.player.getVs().getChunk(first);
            if (chunk != null) {
              if (chunk.index < fromChunk || chunk.index > latestChunk) {
                // continue;
                System.out.println("sending chunk " + chunk.index + " out of protocol bounds: " + fromChunk + "-" + latestChunk);
              }
              network.send(new ChunkMessage(getMessageTag(), network.getAddress(), son, chunk));
              sonChunkUnavailability.get(club).sonAvailability.get(son).updateChunk(chunk.index);
            } else {
              TextLogger.log(network.getAddress(), "requesting nonexistant chunk: " + first + "\n");
            }
          }
        }
      }
    }
  }
  
  @Override public boolean isOverlayConnected() {
    return mainOverlay.isOverlayConnected() && client.player.getVs() != null
        && client.player.getVs().getFirstMissingChunk() > lastReceivedChunkIndex;
  }
}
