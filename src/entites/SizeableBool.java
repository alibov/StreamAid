package entites;

import interfaces.Sizeable;

import java.io.Serializable;

public class SizeableBool implements Sizeable, Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 748925692L;
  public boolean l;
  
  public SizeableBool(final boolean l) {
    this.l = l;
  }
  
  @Override public long getSimulatedSize() {
    return Integer.SIZE; // approx.
  }
  
  @Override public String toString() {
    return Boolean.toString(l);
  }
}
