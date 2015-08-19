package utils.distributions;

import java.util.Random;

public class ConstantDistribution extends Distribution {
  private final double value;
  
  public ConstantDistribution(final double value) {
    this.value = value;
  }
  
  @Override public double generateDistribution(final Random r) {
    return value;
  }
  
  @Override protected String getArgumentsString() {
    return "value=\"" + value + "\"";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(value);
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
    final ConstantDistribution other = (ConstantDistribution) obj;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
      return false;
    }
    return true;
  }
}
