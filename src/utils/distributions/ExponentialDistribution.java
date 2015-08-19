package utils.distributions;

import java.util.Random;

public class ExponentialDistribution extends Distribution {
  private final double mean;
  
  public ExponentialDistribution(final double mean) {
    this.mean = mean;
  }
  
  @Override public double generateDistribution(final Random r) {
    final double U = r.nextDouble();
    return mean * (-Math.log(U));
  }
  
  @Override String getArgumentsString() {
    return "mean=\"" + mean + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(mean);
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
    final ExponentialDistribution other = (ExponentialDistribution) obj;
    if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean)) {
      return false;
    }
    return true;
  }
}
