package entites;

import java.util.Iterator;
import java.util.TreeSet;

import messages.BitmapUpdateMessage;

public class Availability {
  private long earlisetChunkSeen = 0;
  private final TreeSet<Long> nodeAvailability;
  
  public Availability(final Availability av) {
    nodeAvailability = av.nodeAvailability;
    earlisetChunkSeen = av.earlisetChunkSeen;
  }
  
  public Availability(final Bitmap b) {
    if (!b.isEmpty()) {
      nodeAvailability = b.toIndices();
      earlisetChunkSeen = b.getFirstChunk();
    } else {
      nodeAvailability = new TreeSet<Long>();
    }
  }
  
  public void updateBitmap(final BitmapUpdateMessage br, final boolean waitIfNoChunk) {
    nodeAvailability.addAll(br.bitmap.toIndices());
    final long firstChunk = br.bitmap.getFirstChunk();
    if (br.earliestChunkAvailable != -1) {
      if (waitIfNoChunk) {
        for (long i = br.earliestChunkAvailable; i < br.bitmap.startingFrame; ++i) {
          nodeAvailability.add(i);
        }
      }
      if (firstChunk < earlisetChunkSeen) {
        earlisetChunkSeen = firstChunk;
      }
      if (br.earliestChunkAvailable < earlisetChunkSeen) {
        earlisetChunkSeen = br.earliestChunkAvailable;
      }
    } else {
      earlisetChunkSeen = firstChunk;
    }
  }
  
  @Override public String toString() {
    return "" + nodeAvailability;
  }
  
  public boolean hasChunk(final long missingChunk) {
    if (missingChunk < earlisetChunkSeen) {
      return false;
    }
    return nodeAvailability.contains(missingChunk);
  }
  
  public long getEarliestChunkSeen() {
    return earlisetChunkSeen;
  }
  
  public long getLastChunk() {
    return nodeAvailability.last();
  }
  
  public long getEarliestContinuousChunk(final int maxOffset) {
    final Iterator<Long> it = nodeAvailability.descendingIterator();
    long retVal = it.next();
    int i = 0;
    while (it.hasNext()) {
      i++;
      final long newVal = it.next();
      if (newVal != retVal - 1 || i > maxOffset) {
        return retVal;
      }
      retVal = newVal;
    }
    return retVal;
  }
  
  public boolean isEmpty() {
    return nodeAvailability.isEmpty();
  }

  public void updateChunk(final long index) {
    nodeAvailability.add(index);
  }

  public long getFirstMissingChunk() {
    if (isEmpty()) {
      return 1;
    }
    long last = nodeAvailability.first() - 1;
    for (final long chunk : nodeAvailability) {
      if (chunk != last + 1) {
        return last + 1;
      }
      last = chunk;
    }
    return last + 1;
  }
}
