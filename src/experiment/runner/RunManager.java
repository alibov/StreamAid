package experiment.runner;

import java.io.IOException;
import java.util.List;

import experiment.ExperimentConfiguration;

public interface RunManager {
  public abstract int getStartedRuns();
  
  public abstract int getStartedRuns(ExperimentConfiguration expc);
  
  public abstract int getFinishedRuns();
  
  public abstract void closeAll();
  
  public abstract void initFiles();
  
  public abstract void initRun(int i) throws IOException;
  
  public abstract void finishRun(String logAnalyserName) throws IOException;
  
  public abstract List<String> getAnalysers() throws IOException;
}