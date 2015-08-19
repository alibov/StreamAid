package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import utils.Statistics;

class ProtocolInfo implements Serializable {
  private static final long serialVersionUID = 6307986429959791322L;
  long messagesSent;
  long overheadMessagesSent;
  long duplicateChunkMessagesSent;
  long bytesSent;
  long overheadBytesSent;
  long duplicateChunkBytesSent;
  Map<String/* node */, Statistics> nodeDegree = new TreeMap<String, Statistics>();
  double averageAverageDegree;
  double averageNodeDegreeVariance;
  double degreeVariance;
  double serverAverageDegree;
  double serverDegreeVariance;
  Statistics serverStats = new Statistics();
}