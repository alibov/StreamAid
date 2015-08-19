package entites;

import interfaces.Sizeable;

import java.io.Serializable;
import java.util.TreeSet;

import utils.Common;

public class VideoStreamChunk implements Serializable, Sizeable, Comparable<VideoStreamChunk> {
  private static final long serialVersionUID = -4420725537030060927L;
  public final long index;
  
  public VideoStreamChunk(final long index) {
    this.index = index;
  }
  
  @Override public String toString() {
    return "VSC" + index;
  }
  
  @Override public long getSimulatedSize() {
    return Common.currentConfiguration.bitRate + Long.SIZE;
  }
  
  public double getQuality() {
    return 1.0;
  }
  
  public MDCVideoStreamChunk getMDChunk(final TreeSet<Integer> descriptors) {
    if (descriptors == null || descriptors.isEmpty()) {
      throw new RuntimeException("getMDChunk called with illegal input");
    }
    return new MDCVideoStreamChunk(index, descriptors);
  }
  
  public MDCVideoStreamChunk getMDChunk(final int descriptor) {
    return new MDCVideoStreamChunk(index, descriptor);
  }
  
  public TreeSet<Integer> getDescriptions() {
    final TreeSet<Integer> retVal = new TreeSet<Integer>();
    for (Integer i = 0; i < Common.currentConfiguration.descriptions; i++) {
      retVal.add(i);
    }
    return retVal;
  }

  @Override public int compareTo(final VideoStreamChunk arg0) {
    return (new Long(index)).compareTo(arg0.index);
  }
}
