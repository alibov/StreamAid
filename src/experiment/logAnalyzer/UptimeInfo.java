package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import utils.Statistics;

class UptimeInfo implements Serializable {
  private static final long serialVersionUID = -8810734405334807211L;
  Map<String/* protocol */, Statistics/* degree */> protocolDegree = new TreeMap<String, Statistics>();
}