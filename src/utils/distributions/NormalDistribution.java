package utils.distributions;

import java.util.Random;

public class NormalDistribution extends Distribution {
  private final double mean;
  private final double variance;
  
  public NormalDistribution(final double mean, final double variance) {
    this.mean = mean;
    this.variance = variance;
  }
  
  /**
   * the Box�Muller method
   */
  @Override public double generateDistribution(final Random r) {
    final double U = r.nextDouble();
    final double V = r.nextDouble();
    final double N = Math.sqrt(-2 * Math.log(U)) * Math.sin(2 * Math.PI * V);
    return N * Math.sqrt(variance) + mean;
  }
  
  @Override String getArgumentsString() {
    return "mean=\"" + mean + "\" variance=\"" + variance + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(mean);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(variance);
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
    final NormalDistribution other = (NormalDistribution) obj;
    if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean)) {
      return false;
    }
    if (Double.doubleToLongBits(variance) != Double.doubleToLongBits(other.variance)) {
      return false;
    }
    return true;
  }
}
