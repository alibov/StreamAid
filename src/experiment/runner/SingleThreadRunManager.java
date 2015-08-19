package experiment.runner;

import java.io.File;
import java.io.IOException;

import experiment.ExperimentConfiguration;
import utils.Common;

public class SingleThreadRunManager extends LocalRunManager {
  private int startedRuns = 0;
  
  @Override public int getStartedRuns(final ExperimentConfiguration expc) {
    return startedRuns;
  }
  
  @Override public int getFinishedRuns() {
    return startedRuns;
  }
  
  @Override public void initRun(final int i) throws IOException {
    startedRuns++;
    analysersFile = new File(Common.currentConfiguration.name + File.separator + "analysers");
  }
}
