package entites;

import interfaces.Sizeable;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import modules.player.VideoStream;

public class Bitmap implements Serializable, Sizeable {
  private static final long serialVersionUID = 1305561111888583232L;
  public final byte[] bitmap;
  public final long startingFrame;

  public Bitmap(final Set<Integer> bitmap, final long startingFrame) {
    int bitmapSize = 1;
    if (!bitmap.isEmpty()) {
      bitmapSize = Collections.max(bitmap) / Byte.SIZE + 1;
    }
    this.bitmap = new byte[bitmapSize];
    for (int i = 0; i < this.bitmap.length; i++) {
      this.bitmap[i] = 0;
    }
    for (final Integer in : bitmap) {
      this.bitmap[in / Byte.SIZE] |= 1 << in % Byte.SIZE;
    }
    this.startingFrame = startingFrame;
  }

  private static Set<Integer> getSubstractionSet(final Set<Long> in) {
    final Set<Integer> retVal = new TreeSet<Integer>();
    final long first = in.iterator().next();
    for (final Long n : in) {
      retVal.add((int) (n - first));
    }
    return retVal;
  }

  public Bitmap(final Set<Long> chunkIndices) {
    this(getSubstractionSet(chunkIndices), chunkIndices.iterator().next());
  }

  public Bitmap(final long index) {
    this(Collections.singleton(0), index);
  }
  
  public boolean hasChunk(final long missingChunk, final boolean waitIfNoChunk) {
    if (missingChunk < startingFrame) {
      if (!waitIfNoChunk) {
        return false;
      }
      return missingChunk > startingFrame - VideoStream.safetyBuffer + 2;
    }
    final int place = (int) (missingChunk - startingFrame);
    if (place / Byte.SIZE >= bitmap.length) {
      return false;
    }
    return (bitmap[place / Byte.SIZE] & (1 << place % Byte.SIZE)) != 0;
  }

  @Override public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Bitmap [ bitmap=");
    for (int i = 0; i < bitmap.length * Byte.SIZE; i++) {
      if ((bitmap[i / 8] & (1 << i % Byte.SIZE)) != 0) {
        sb.append(startingFrame + i + ", ");
      }
    }
    sb.append("]");
    return sb.toString();
  }

  public long getFirstChunk() {
    for (int i = 0; i < bitmap.length; i++) {
      if (bitmap[i] != 0) {
        Byte b = (byte) (new Byte(bitmap[i]) & Byte.MAX_VALUE);
        for (int j = 0; j < Byte.SIZE; j++, b = (byte) (b >> 1)) {
          if (b % 2 == 1) {
            return startingFrame + i * Byte.SIZE + j;
          }
        }
        return startingFrame + i * Byte.SIZE + Byte.SIZE - 1;
      }
    }
    return Long.MAX_VALUE;
  }

  public boolean isEmpty() {
    for (final byte element : bitmap) {
      if (element != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   *
   * @return last chunk of continuous data. startingFrame-1 if empty bitmap
   */
  public long getLastContinousChunk() {
    int i;
    for (i = 0; i < bitmap.length && bitmap[i] != 0; i++) {
      // find first non zero bitmap
    }
    i--;
    if (i == -1) {
      return -1;
    }
    Byte b = new Byte(bitmap[i]);
    int j;
    for (j = 0; j < Byte.SIZE && b % 2 == 1; j++, b = (byte) (b >> 1)) {
      // find first non zero bit
    }
    return startingFrame + i * Byte.SIZE + j - 1;
  }

  public long getLastChunk() {
    int byteNumber = -1;
    for (int i = 0; i < bitmap.length; i++) {
      if (bitmap[i] != 0) {
        byteNumber = i;
      }
    }
    if (byteNumber == -1) {
      return startingFrame - 1;
    }
    Byte b = new Byte(bitmap[byteNumber]);
    int bitNumber = 0;
    if (b < 0) { // bytes are signed!
      bitNumber = Byte.SIZE - 1;
    } else {
      for (int j = 0; j < Byte.SIZE; j++, b = (byte) (b >> 1)) {
        if (b % 2 == 1) {
          bitNumber = j;
        }
      }
    }
    return startingFrame + byteNumber * Byte.SIZE + bitNumber;
  }

  @Override public long getSimulatedSize() {
    return Long.SIZE + bitmap.length * Byte.SIZE;
  }

  public TreeSet<Long> toIndices() {
    final TreeSet<Long> retVal = new TreeSet<Long>();
    for (int i = 0; i < bitmap.length; ++i) {
      if (bitmap[i] < 0) {
        retVal.add(startingFrame + i * Byte.SIZE + Byte.SIZE - 1);
      }
      for (int j = 0; j < Byte.SIZE - 1; j++) {
        if (((bitmap[i] & Byte.MAX_VALUE) >> j) % 2 == 1) {
          retVal.add(startingFrame + i * Byte.SIZE + j);
        }
      }
    }
    return retVal;
  }
}
