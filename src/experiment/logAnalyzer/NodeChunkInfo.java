package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class NodeChunkInfo implements Serializable {
  private static final long serialVersionUID = -8942571413860417549L;
  public double playTime;
  public double quality;
  public Map<Integer, Integer> descToOrder = new TreeMap<Integer, Integer>();
  // TODO populate
  public Map<Integer, Double> descToArriveLatency = new TreeMap<Integer, Double>();
  public double playLatency;
}