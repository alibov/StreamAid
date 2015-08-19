package entites;

import java.util.LinkedList;
import java.util.Set;

import modules.player.StreamListener;

public class BitmapListener implements StreamListener {
  private final Integer numOfSubstreams;
  private final Long[] bitmapFirstPart;
  // saves within its head the oldest arrived chunk. update the data lazily.
  private final LinkedList<Long> oldestArrivedChunkQueue;
  private Long latestArrivedChunk;
  
  public BitmapListener(final Integer numOfSubstreams) {
    if (numOfSubstreams == null) {
      throw new IllegalArgumentException("maxSize mustn't be null");
    }
    this.numOfSubstreams = numOfSubstreams;
    bitmapFirstPart = new Long[numOfSubstreams];
    for (int i = 0; i < this.numOfSubstreams; i++) {
      bitmapFirstPart[i] = 0L;
    }
    oldestArrivedChunkQueue = new LinkedList<Long>();
    oldestArrivedChunkQueue.add(0L);
    latestArrivedChunk = 0L;
  }
  
  @Override public void onStreamUpdate(final Set<Long> updatedChunks) {
    for (final Long chunk : updatedChunks) {
      final int chunkSubStream = (int) (chunk % numOfSubstreams);
      if (chunk > bitmapFirstPart[chunkSubStream]) {
        bitmapFirstPart[chunkSubStream] = chunk;
      }
      // takes care of latest arrived chunk statistics
      if (latestArrivedChunk < chunk) {
        latestArrivedChunk = chunk;
      }
    }
    // takes care of oldest arrived chunk statistics
    while (!oldestArrivedChunkQueue.isEmpty()) {
      final int oldestChunkSubStream = (int) (oldestArrivedChunkQueue.peek() % numOfSubstreams);
      // if the data in the head of the heap is correct, break from this loop.
      // else update this data.
      if (bitmapFirstPart[oldestChunkSubStream] != oldestArrivedChunkQueue.peek()) {
        oldestArrivedChunkQueue.poll();
        oldestArrivedChunkQueue.add(bitmapFirstPart[oldestChunkSubStream]);
      } else {
        break;
      }
    }
  }
  
  public Long getChunkDeviation() {
    return latestArrivedChunk - oldestArrivedChunkQueue.peek();
  }
  
  public Long getLatestChunk() {
    return latestArrivedChunk;
  }
  
  public Long[] getFirstPartOfBitmap() {
    return bitmapFirstPart;
  }
  
  @Override public String toString() {
    String str = new String();
    int i = 0;
    str += "[";
    for (final Long l : bitmapFirstPart) {
      str += l;
      i++;
      if (i != bitmapFirstPart.length) {
        str += ", ";
      }
    }
    str += "]";
    return str;
  }
}
