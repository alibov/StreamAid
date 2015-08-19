package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * utility class to compute mean rounds the reounds are weibul distributed
 * 
 * @author Alexander Libov
 */
public class Churner {
  /**
   * parameters: k = 0.34, lambda = 21.3 for Red Hat, k = 0.38, lambda = 42.4
   * for Debian k = 0.59, lambda = 41.9 for FlatOut.
   */
  private final double k = 0.5;
  private final double mean;
  private final double lambda;
  private final int peers;
  List<Double> nums = new ArrayList<Double>();
  
  public Churner(final int churnMean) {
    mean = churnMean;
    lambda = mean / 2;
    peers = Common.currentConfiguration.nodes;
    for (double i = (double) 1 / (peers + 1); i < 1; i += (double) 1 / (peers + 1)) {
      nums.add(i);
    }
  }
  
  public double uniformToWeibull(final double num) {
    return (lambda) * Math.pow(-(Math.log(1 - num)), 1 / k);
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(k);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(lambda);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(mean);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((nums == null) ? 0 : nums.hashCode());
    result = prime * result + peers;
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
    final Churner other = (Churner) obj;
    if (Double.doubleToLongBits(k) != Double.doubleToLongBits(other.k)) {
      return false;
    }
    if (Double.doubleToLongBits(lambda) != Double.doubleToLongBits(other.lambda)) {
      return false;
    }
    if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean)) {
      return false;
    }
    if (nums == null) {
      if (other.nums != null) {
        return false;
      }
    } else if (!nums.equals(other.nums)) {
      return false;
    }
    if (peers != other.peers) {
      return false;
    }
    return true;
  }
}
