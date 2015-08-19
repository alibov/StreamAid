package utils.distributions;

import java.util.Random;

public class ParetoDistribution extends Distribution {
  private final double Xm;
  private final double alpha;
  
  public ParetoDistribution(final double xm, final double alpha) {
    if (xm <= 0 || alpha <= 0) {
      throw new RuntimeException("illegal arguments for pareto distribution: " + xm + "," + alpha);
    }
    Xm = xm;
    this.alpha = alpha;
  }
  
  @Override public double generateDistribution(final Random r) {
    final double U = 1.0 - r.nextDouble();
    return Xm / (Math.pow(U, 1 / alpha));
  }
  
  @Override String getArgumentsString() {
    return "Xm=\"" + Xm + "\" alpha=\"" + alpha + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(Xm);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(alpha);
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
    final ParetoDistribution other = (ParetoDistribution) obj;
    if (Double.doubleToLongBits(Xm) != Double.doubleToLongBits(other.Xm)) {
      return false;
    }
    if (Double.doubleToLongBits(alpha) != Double.doubleToLongBits(other.alpha)) {
      return false;
    }
    return true;
  }
}
