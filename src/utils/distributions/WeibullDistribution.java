package utils.distributions;

import java.util.Random;

public class WeibullDistribution extends Distribution {
  /**
   * parameters: k = 0.34, lambda = 21.3 for Red Hat, k = 0.38, lambda = 42.4
   * for Debian k = 0.59, lambda = 41.9 for FlatOut.
   */
  private final double lambda;
  private final double k;
  
  public WeibullDistribution(final double lambda, final double k) {
    this.lambda = lambda;
    this.k = k;
  }
  
  public double uniformToDistribution(final double num) {
    return (lambda) * Math.pow(-(Math.log(1 - num)), 1 / k);
  }
  
  @Override public double generateDistribution(final Random r) {
    return uniformToDistribution(r.nextDouble());
  }
  
  @Override String getArgumentsString() {
    return "lambda=\"" + lambda + "\" k=\"" + k + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(k);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lambda);
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
    final WeibullDistribution other = (WeibullDistribution) obj;
    if (Double.doubleToLongBits(k) != Double.doubleToLongBits(other.k)) {
      return false;
    }
    if (Double.doubleToLongBits(lambda) != Double.doubleToLongBits(other.lambda)) {
      return false;
    }
    return true;
  }
}
