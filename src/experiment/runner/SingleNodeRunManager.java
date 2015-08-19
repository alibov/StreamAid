package experiment.runner;

import java.io.IOException;
import java.util.List;

import experiment.ExperimentConfiguration;
import utils.Common;

public class SingleNodeRunManager implements RunManager {
  boolean runStarted = false;
  
  @Override public int getStartedRuns() {
    return getStartedRuns(Common.currentConfiguration);
  }
  
  @Override public int getStartedRuns(final ExperimentConfiguration expc) {
    return runStarted ? expc.runs : 0;
  }
  
  @Override public int getFinishedRuns() {
    return Common.currentConfiguration.runs;
  }
  
  @Override public void closeAll() {
    // do nothing
  }
  
  @Override public void initFiles() {
    // do nothing
  }
  
  @Override public void initRun(final int i) throws IOException {
    runStarted = true;
  }
  
  @Override public void finishRun(final String logAnalyserName) throws IOException {
    // do nothing
  }
  
  @Override public List<String> getAnalysers() throws IOException {
    throw new RuntimeException("shouldn't get here");
  }
}
