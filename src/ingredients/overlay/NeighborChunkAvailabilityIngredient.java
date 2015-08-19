package ingredients.overlay;

import ingredients.AbstractIngredient;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.BitmapUpdateMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import modules.player.StreamListener;
import utils.Utils;
import entites.Availability;
import entites.Bitmap;
import experiment.frameworks.NodeAddress;

//TODO merge with info exchange ingredient
public class NeighborChunkAvailabilityIngredient extends AbstractIngredient<OverlayModule<?>> implements StreamListener {
  public enum OperationMode {
    updateEveryRound, UpdateEveryChunk, none
  }
  
  public final boolean serverSendsBitmaps;
  private long minUpdatedChunk = Long.MAX_VALUE;
  private OperationMode opMode;
  public Map<NodeAddress, Availability> neighborAvailability = new TreeMap<NodeAddress, Availability>();
  public final Map<NodeAddress, Long> lastAvailabilitySent = new TreeMap<NodeAddress, Long>();
  private boolean firstChunkReceived = false;
  
  public Availability getNeighborAvailability(final NodeAddress neighbor) {
    return neighborAvailability.get(neighbor);
  }
  
  public NeighborChunkAvailabilityIngredient(final OperationMode opMode, final boolean serverSendsBitmaps, final Random r) {
    super(r);
    this.serverSendsBitmaps = serverSendsBitmaps;
    this.opMode = opMode;
  }
  
  public void setOperationMode(final OperationMode opMode) {
    this.opMode = opMode;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!alg.isOverlayConnected()) {
      updateNeighborAvailability();
      return;
    }
    // updateNeighbors();
    if (!client.isServerMode() && firstChunkReceived) {
      sendBitmaps();
    }
    if (client.isServerMode() && serverSendsBitmaps && Utils.getMovieTime() >= 0 && !opMode.equals(OperationMode.none)) {
      for (final NodeAddress neighbor : alg.getNeighbors()) {
        final Set<Integer> bits = new TreeSet<Integer>();
        bits.add(0);
        final Bitmap bitmap = new Bitmap(bits, client.player.getVs().windowOffset);
        client.network.send(new BitmapUpdateMessage(getMessageTag(), client.network.getAddress(), neighbor, bitmap, 1));
      }
    }
    updateNeighborAvailability();
    minUpdatedChunk = Long.MAX_VALUE;
  }
  
  private void updateNeighborAvailability() {
    if (client.player.getVs() != null) {
      final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
      for (final NodeAddress n : neighborAvailability.keySet()) {
        // removing useless neighbor availability records
        if (neighborAvailability.get(n).getEarliestChunkSeen() == Long.MAX_VALUE || neighborAvailability.get(n).isEmpty()
            || neighborAvailability.get(n).getLastChunk() < client.player.getVs().windowOffset || !client.network.isUp(n)) {
          toRemove.add(n);
        }
      }
      neighborAvailability.keySet().removeAll(toRemove);
    }
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final OverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
    client.player.addVSlistener(this);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    client.player.removeVSlistener(this);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    firstChunkReceived = true;
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof BitmapUpdateMessage) {
      final BitmapUpdateMessage br = (BitmapUpdateMessage) message;
      if (br.isEmpty()) {
        // TODO remove this empty bit map before sending to Alex
        // System.err.println(Utils.getMovieTime() + ": " +
        // client.node.toString() + ": got empty bitmap!");
        return;
      }
      Utils.checkExistence(neighborAvailability, message.sourceId, new Availability(br.bitmap));
      neighborAvailability.get(message.sourceId).updateBitmap(br, client.player.waitIfNoChunk);
    }
  }
  
  private void sendBitmaps() {
    final long offset = client.player.getVs().windowOffset;
    for (final NodeAddress nsi : alg.getNeighbors()) {
      if (nsi.equals(client.network.getServerNode())) {
        continue;
      }
      if (lastAvailabilitySent.containsKey(nsi) && opMode.equals(OperationMode.UpdateEveryChunk)) {
        continue;// send once with earliest chunk
      }
      long earliestChunk;
      long bitmapStart;
      earliestChunk = client.player.getVs().getEarliestChunk();
      if (earliestChunk == -1L) { // no chunks!
        return;
      }
      if (client.player.waitIfNoChunk) {
        bitmapStart = offset;
      } else {
        if (lastAvailabilitySent.get(nsi) == null) {
          bitmapStart = earliestChunk;
        } else {
          bitmapStart = minUpdatedChunk;
          if (bitmapStart == Long.MAX_VALUE) {
            // no new chunks - don't send a bitmap
            continue;
          }
        }
      }
      final Bitmap bitmap = client.player.getVs().getBitmap(bitmapStart);
      if (bitmap.isEmpty() && earliestChunk == offset) {
        System.err.println(Utils.getMovieTime() + ": " + client.network.getAddress().toString() + ": sending empty bitmap!");
        continue;
      }
      client.network.send(new BitmapUpdateMessage(getMessageTag(), client.network.getAddress(), nsi, bitmap, earliestChunk));
      lastAvailabilitySent.put(nsi, bitmapStart);
    }
    lastAvailabilitySent.keySet().retainAll(alg.getNeighbors());
  }
  
  @Override public void onStreamUpdate(final Set<Long> updatedChunks) {
    minUpdatedChunk = Math.min(minUpdatedChunk, Collections.min(updatedChunks));
    firstChunkReceived = true;
    if (opMode.equals(OperationMode.UpdateEveryChunk)) {
      final Bitmap b = new Bitmap(updatedChunks);
      for (final NodeAddress nsi : alg.getNeighbors()) {
        client.network.send(new BitmapUpdateMessage(getMessageTag(), client.network.getAddress(), nsi, b, Collections
            .min(updatedChunks)));
      }
    }
  }
}
