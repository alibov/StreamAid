package experiment.runner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import experiment.ExperimentConfiguration;
import logging.ObjectLogger;
import logging.TextLogger;
import utils.Common;
import utils.Utils;

public class LocalRunManager implements RunManager {
  private File threadsFile = null;
  protected File analysersFile = null;
  
  @Override public int getStartedRuns() {
    return getStartedRuns(Common.currentConfiguration);
  }
  
  @Override public int getStartedRuns(final ExperimentConfiguration expc) {
    int retVal = 0;
    final File dir = new File(expc.name);
    if (!dir.exists()) {
      return retVal;
    }
    if (threadsFile == null || !threadsFile.exists()) {
      return 0;
    }
    try {
      final BufferedReader br = new BufferedReader(new FileReader(threadsFile));
      String line;
      while ((line = br.readLine()) != null) {
        if (!line.contains("done")) {
          retVal++;
        }
      }
      br.close();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return retVal;
  }
  
  @Override public int getFinishedRuns() {
    int retVal = 0;
    try {
      final BufferedReader br = new BufferedReader(new FileReader(threadsFile));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.contains("done")) {
          retVal++;
        }
      }
      br.close();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return retVal;
  }
  
  @Override public void closeAll() {
    if (threadsFile != null) {
      threadsFile.delete();
      threadsFile = null;
    }
    if (analysersFile != null) {
      if (TextLogger.disabled && TextLogger.logNodes.isEmpty()) {
        try {
          for (final String a : getAnalysers()) {
            Utils.deleteRecursive(new File(a).getParentFile());
          }
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
      analysersFile.delete();
      analysersFile = null;
    }
  }
  
  /* (non-Javadoc)
   * 
   * @see experiment.RunManager#initFiles() */
  @Override public void initFiles() {
    threadsFile = new File(Common.currentConfiguration.name + File.separator + Common.currentConfiguration.toString() + "threads");
    analysersFile = new File(Common.currentConfiguration.name + File.separator + Common.currentConfiguration.toString()
        + "analysers");
  }
  
  @Override public void initRun(final int i) throws IOException {
    initFiles();
    final BufferedWriter bw = new BufferedWriter(new FileWriter(threadsFile, true));
    bw.append(Thread.currentThread().getId() + "->" + i + "\n");
    bw.close();
  }
  
  /* (non-Javadoc)
   * 
   * @see experiment.RunManager#finishRun(java.lang.String) */
  @Override public void finishRun(final String logAnalyserName) throws IOException {
    if (threadsFile != null) {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(threadsFile, true));
      bw.append(Thread.currentThread().getId() + " done\n");
      bw.close();
    }
    if (analysersFile != null) {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(analysersFile, true));
      bw.append(logAnalyserName + "\n");
      bw.close();
    }
    ObjectLogger.objLists = new HashMap<String, List<?>>();
  }
  
  @Override public List<String> getAnalysers() throws IOException {
    if (!analysersFile.exists()) {
      return new LinkedList<String>();
    }
    final BufferedReader br = new BufferedReader(new FileReader(analysersFile));
    String line;
    final List<String> analyzers = new LinkedList<String>();
    while ((line = br.readLine()) != null) {
      analyzers.add(line);
    }
    br.close();
    return analyzers;
  }
}
