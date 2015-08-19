package experiment.logAnalyzer;

import java.io.Serializable;

class OrderInfo implements Serializable {
  private static final long serialVersionUID = -87945944720126098L;
  double averageLatency;
  int count;
}