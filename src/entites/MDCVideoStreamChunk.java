package entites;

import java.util.TreeSet;

import utils.Common;

public class MDCVideoStreamChunk extends VideoStreamChunk {
  private static final long serialVersionUID = -3349906661286613300L;
  private final TreeSet<Integer> descriptions;
  
  public MDCVideoStreamChunk(final long index, final TreeSet<Integer> descriptions) {
    super(index);
    if (descriptions == null) {
      throw new RuntimeException("null descriptions passed!");
    }
    this.descriptions = descriptions;
  }
  
  public MDCVideoStreamChunk(final long index, final int descriptor) {
    super(index);
    descriptions = new TreeSet<Integer>();
    descriptions.add(descriptor);
  }
  
  public void combineChunk(final MDCVideoStreamChunk chunk) {
    if (chunk.index != index) {
      return; // throw?
    }
    descriptions.addAll(chunk.descriptions);
  }
  
  @Override public double getQuality() {
    return ((double) descriptions.size()) / Common.currentConfiguration.descriptions;
  }
  
  @Override public MDCVideoStreamChunk getMDChunk(final TreeSet<Integer> descriptors) {
    if (descriptions.containsAll(descriptors)) {
      return new MDCVideoStreamChunk(index, descriptors);
    }
    throw new RuntimeException("getMDChunk called with illegal descriptorss");
  }
  
  @Override public TreeSet<Integer> getDescriptions() {
    return descriptions;
  }
  
  @Override public String toString() {
    return "MDC-VSC" + index + " " + descriptions;
  }
}
