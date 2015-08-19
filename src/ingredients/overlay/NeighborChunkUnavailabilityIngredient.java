package ingredients.overlay;

import ingredients.AbstractIngredient;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.BitmapUpdateMessage;
import messages.ChunkMessage;
import messages.ChunkReceivedMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.MultipleTreeOverlayModule;
import utils.Common;
import entites.Availability;
import entites.Bitmap;
import experiment.frameworks.NodeAddress;

public class NeighborChunkUnavailabilityIngredient extends AbstractIngredient<MultipleTreeOverlayModule<?>> {
  public Map<NodeAddress, Availability> sonAvailability = new TreeMap<NodeAddress, Availability>();
  private final Set<NodeAddress> fathersNotified = new HashSet<NodeAddress>();
  Set<Long> receivedChunks = new HashSet<Long>();
  protected final int club;
  
  public NeighborChunkUnavailabilityIngredient(final int club, final Random r) {
    super(r);
    this.club = club;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final MultipleTreeOverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
    client.network.addListener(this, ChunkMessage.class);
  }
  
  @Override public String getMessageTag() {
    return super.getMessageTag() + club;
  }
  
  protected Set<NodeAddress> getNodeGroup() {
    return alg.getNodeGroup(alg.getDownloadGroupName(club));
  }
  
  protected boolean shouldNotifyGroup(final ChunkMessage message) {
    return message.chunk.index % Common.currentConfiguration.descriptions == club;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    fathersNotified.retainAll(getNodeGroup());
    final Set<NodeAddress> toNotify = new TreeSet<NodeAddress>(getNodeGroup());
    toNotify.removeAll(fathersNotified);
    for (final NodeAddress father : toNotify) {
      notifyFather(father);
    }
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : sonAvailability.keySet()) {
      // removing useless neighbor availability records
      if (!client.network.isUp(n)) {
        toRemove.add(n);
      }
    }
    sonAvailability.keySet().removeAll(toRemove);
    final TreeSet<NodeAddress> removed = new TreeSet<NodeAddress>(sonAvailability.keySet());
    removed.removeAll(alg.getNodeGroup(alg.getUploadGroupName(club)));
    // sonAvailability.keySet().retainAll(overlay.getNodeGroup(overlay.getUploadGroupName(club)));
    // TextLogger.log(client.node.getImpl(), "club: " + club + " removing: " +
    // removed + "\n");
  }
  
  private void notifyFather(final NodeAddress father) {
    // Bitmap bitmap = new Bitmap(new TreeSet<Integer>(), 0);
    if (client.player.getVs() != null) {
      final Bitmap bitmap = client.player.getVs().getBitmap(client.player.getVs().getFirstMissingChunk() - 1);
      client.network.send(new BitmapUpdateMessage(getMessageTag(), client.network.getAddress(), father, bitmap, -1));
      fathersNotified.add(father);
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof BitmapUpdateMessage) {
      if (sonAvailability.containsKey(message.sourceId)) {
        sonAvailability.get(message.sourceId).updateBitmap((BitmapUpdateMessage) message, client.player.waitIfNoChunk);
      } else {
        sonAvailability.put(message.sourceId, new Availability(((BitmapUpdateMessage) message).bitmap));
      }
    } else if (message instanceof ChunkMessage) {
      if (!receivedChunks.contains(((ChunkMessage) message).chunk.index) && shouldNotifyGroup((ChunkMessage) message)) {
        receivedChunks.add(((ChunkMessage) message).chunk.index);
        for (final NodeAddress father : getNodeGroup()) {
          if (!fathersNotified.contains(father)) {
            notifyFather(father);
            fathersNotified.add(father);
          } else {
            client.network.send(new ChunkReceivedMessage(getMessageTag(), client.network.getAddress(), father,
                ((ChunkMessage) message).chunk.index));
          }
        }
      }
    } else if (message instanceof ChunkReceivedMessage) {
      if (!sonAvailability.containsKey(message.sourceId)) {
        // throw new RuntimeException("son has no availability!");
        TextLogger.log(client.network.getAddress(), "WARN: son" + message.sourceId + "has no availability\n");
        return;
      }
      sonAvailability.get(message.sourceId).updateChunk(((ChunkReceivedMessage) message).index);
    }
  }
}
