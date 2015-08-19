package utils.distributions;

import java.util.Random;

public class UniformDistribution extends Distribution {
  private final double min;
  private final double max;
  
  public UniformDistribution(final double min, final double max) {
    this.min = min;
    this.max = max;
  }
  
  /**
   * the Box�Muller method
   */
  @Override public double generateDistribution(final Random r) {
    return r.nextDouble() * (max - min) + min;
  }
  
  @Override String getArgumentsString() {
    return "min=\"" + min + "\" max=\"" + max + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(max);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(min);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
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
    final UniformDistribution other = (UniformDistribution) obj;
    if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max)) {
      return false;
    }
    if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min)) {
      return false;
    }
    return true;
  }
}
