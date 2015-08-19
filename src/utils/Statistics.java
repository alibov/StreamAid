package utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Vector;

public class Statistics implements Serializable {
  private static final long serialVersionUID = -2240437045390692179L;
  Vector<Double> data = new Vector<Double>();
  Vector<Double> weights = new Vector<Double>();
  double maxDatum = Double.NEGATIVE_INFINITY;
  String maxItem;
  double minDatum = Double.POSITIVE_INFINITY;
  String minItem;
  
  public void addDatum(final double datum) {
    addDatum(datum, null);
  }
  
  public void addDatum(final double datum, final String item) {
    data.add(datum);
    if (datum > maxDatum) {
      maxDatum = datum;
      maxItem = item;
    }
    if (datum < minDatum) {
      minDatum = datum;
      minItem = item;
    }
  }
  
  public double getMean() {
    double sum = 0.0;
    double weightsSum = 0.0;
    for (int i = 0; i < data.size(); i++) {
      if (weights.size() > 0) {
        sum += weights.get(i) * data.get(i);
        weightsSum += weights.get(i);
      } else {
        sum += data.get(i);
      }
    }
    if (weightsSum == 0.0) {
      return sum / data.size();
    }
    return sum / weightsSum;
  }
  
  public double getVariance() {
    final double mean = getMean();
    double temp = 0.0;
    double weightsSum = 0.0;
    for (int i = 0; i < data.size(); i++) {
      final double a = data.get(i);
      if (weights.size() > 0) {
        temp += weights.get(i) * (mean - a) * (mean - a);
        weightsSum += weights.get(i);
      } else {
        temp += (mean - a) * (mean - a);
      }
    }
    if (weightsSum == 0.0) {
      return temp / data.size();
    }
    return temp / weightsSum;
  }
  
  public double getStdDev() {
    return Math.sqrt(getVariance());
  }
  
  public double getMedian() {
    Collections.sort(data);
    if (data.size() % 2 == 0) {
      return (data.get((data.size() / 2) - 1) + data.get(data.size() / 2)) / 2;
    }
    return data.get(data.size() / 2);
  }
  
  public double dataSize() {
    if (weights.isEmpty()) {
      return data.size();
    }
    double sum = 0;
    for (final Double w : weights) {
      sum += w;
    }
    return sum;
  }
  
  public int getCount() {
    return data.size();
  }
  
  public String getMaxItem() {
    return maxItem;
  }
  
  public void addWeightedDatum(final double weight, final double datum) {
    data.add(datum);
    weights.add(weight);
  }
}