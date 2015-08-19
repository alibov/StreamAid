package entites;

import interfaces.Sizeable;

public class SizeableInt implements Sizeable {
  public int i;
  
  public SizeableInt(final int i) {
    this.i = i;
  }
  
  @Override public long getSimulatedSize() {
    return Integer.SIZE;
  }
  
  @Override public String toString() {
    return Integer.toString(i);
  }
}
