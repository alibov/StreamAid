package utils.distributions;

import java.util.Random;

public abstract class Distribution {
  public int fromTime = Integer.MIN_VALUE;
  
  public abstract double generateDistribution(Random r);
  
  public String toXml() {
    return "<distribution type=\"" + getClass().getSimpleName() + "\"" + " " + getArgumentsString()
        + (fromTime != Integer.MIN_VALUE ? " from=\"" + fromTime + "\"" : "") + "/>\n";
  }
  
  abstract String getArgumentsString();
  
  @Override abstract public boolean equals(Object obj);
  
  @Override abstract public int hashCode();
}
