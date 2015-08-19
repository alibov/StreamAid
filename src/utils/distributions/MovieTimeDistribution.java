package utils.distributions;

import java.util.ArrayList;
import java.util.Random;

import utils.Utils;

public class MovieTimeDistribution extends Distribution {
  private final ArrayList<Distribution> distributions;
  
  public MovieTimeDistribution(final ArrayList<Distribution> distributions) {
    this.distributions = distributions;
  }
  
  @Override public double generateDistribution(final Random r) {
    final long movieTime = Utils.getMovieTime() / 1000;
    int i = 1;
    for (; i < distributions.size(); ++i) {
      if (movieTime < distributions.get(i).fromTime) {
        return distributions.get(i - 1).generateDistribution(r);
      }
    }
    return distributions.get(i - 1).generateDistribution(r);
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((distributions == null) ? 0 : distributions.hashCode());
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
    final MovieTimeDistribution other = (MovieTimeDistribution) obj;
    if (distributions == null) {
      if (other.distributions != null) {
        return false;
      }
    } else if (!distributions.equals(other.distributions)) {
      return false;
    }
    return true;
  }
  
  @Override public String toXml() {
    final StringBuffer retVal = new StringBuffer();
    retVal.append("<distribution type=\"" + getClass().getSimpleName() + "\">\n");
    for (final Distribution d : distributions) {
      retVal.append(d.toXml());
    }
    retVal.append("</distribution>");
    return retVal.toString();
  }
  
  @Override protected String getArgumentsString() {
    throw new RuntimeException("shouldn't be called");
  }
}
