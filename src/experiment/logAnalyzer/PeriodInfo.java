package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.TreeMap;

class PeriodInfo implements Serializable {
  private static final long serialVersionUID = -87945940126096L;
  TreeMap<Integer, Integer> optionChosen = new TreeMap<Integer, Integer>();
  int optionChange = 0;
  int nodesStartingPeriod = 0;
}