package utils.distributions;

import java.util.Random;

public class LogNormalDistribution extends Distribution {
  private final NormalDistribution normalDistr;
  
  public LogNormalDistribution(final double mean, final double variance) {
    normalDistr = new NormalDistribution(mean, variance);
  }
  
  @Override public double generateDistribution(final Random r) {
    return Math.exp(normalDistr.generateDistribution(r));
  }
  
  @Override protected String getArgumentsString() {
    return normalDistr.getArgumentsString();
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((normalDistr == null) ? 0 : normalDistr.hashCode());
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
    final LogNormalDistribution other = (LogNormalDistribution) obj;
    if (normalDistr == null) {
      if (other.normalDistr != null) {
        return false;
      }
    } else if (!normalDistr.equals(other.normalDistr)) {
      return false;
    }
    return true;
  }
}
