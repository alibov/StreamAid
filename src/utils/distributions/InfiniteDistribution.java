package utils.distributions;

import java.util.Random;

public class InfiniteDistribution extends Distribution {
  @Override public double generateDistribution(final Random r) {
    return Double.POSITIVE_INFINITY;
  }
  
  @Override protected String getArgumentsString() {
    return "";
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    return (getClass() == obj.getClass());
  }
  
  @Override public int hashCode() {
    return 0;
  }
}
