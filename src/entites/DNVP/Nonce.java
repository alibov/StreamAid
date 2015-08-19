package entites.DNVP;

import java.util.Random;

public class Nonce {
  // private final Integer nonce;
  private final byte[] nonce;
  public static final int SIZE = 2 * 8;
  
  private Nonce(final byte[] nonce) {
    this.nonce = nonce;
  }
  
  public static Nonce generateNonce(final Random r) {
    final byte[] byteArray = new byte[2];
    r.nextBytes(byteArray);
    return new Nonce(byteArray);
  }
  
  @Override public String toString() {
    final java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(nonce);
    final short s = b.getShort();
    return String.valueOf(s);
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return java.util.Arrays.equals(nonce, ((Nonce) obj).nonce);
  }
  
  @Override public int hashCode() {
    return toString().hashCode();
  }
}
