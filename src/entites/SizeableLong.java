package entites;

import interfaces.Sizeable;

public class SizeableLong implements Sizeable {
  public long l;
  
  public SizeableLong(final long l) {
    this.l = l;
  }
  
  @Override public long getSimulatedSize() {
    return Long.SIZE;
  }
  
  @Override public String toString() {
    return Long.toString(l);
  }
}
