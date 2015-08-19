package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import logging.logObjects.AvailabilityLog;
import logging.logObjects.AvailabilityLog.State;

class SecondInfo implements Serializable {
  private static final long serialVersionUID = -87945944720126096L;
  long serverUploadUtilization;
  long serverBitsInQueue;
  long allUploadUtilization;
  long allBitsInQueue;
  int availablenodes;
  Map<State, Integer> nodesInState = new TreeMap<AvailabilityLog.State, Integer>();
  double latencyAvg = 0;
  TreeMap<Integer, Integer> optionChosen = new TreeMap<Integer, Integer>();
  int optionChange = 0;
}