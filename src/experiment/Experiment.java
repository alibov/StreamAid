package experiment;

import ingredients.streaming.optional.ChunkChoiceMeasurement;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import logging.ObjectLogger;
import logging.TextLogger;
import utils.Common;
import utils.Utils;
import experiment.logAnalyzer.LogAnalyzer;
import experiment.runner.RunManager;

/**
 * an xml file describing the experiment should be prepared Main experiment
 * class that should be run in order to run an experiment. Can be run multiple
 * times to parallelize runs.
 * 
 * @author Alexander Libov
 * 
 */
public class Experiment {
  public static LogAnalyzer results = null;
  public static boolean keepResults = false;
  private static RunManager runManager;
  
  public static void main(final String args[]) throws Exception {
    // System.err.println("running ver 17:00 18/03/13");
    if (args.length < 1) {
      throw new IllegalStateException("usage: program <xml file name>");
    }
    for (int i = 1; i < args.length; i++) {
      TextLogger.logNodes.add(args[i]);
    }
    final File file = new File(args[0]);
    try {
      runConfiguration(file);
    } catch (final Throwable t) {
      t.printStackTrace();
    } finally {
      ObjectLogger.closeAll();
      TextLogger.closeAll();
      if (runManager != null) {
        runManager.closeAll();
      }
    }
  }
  
  public static void runConfiguration(final File configFile) throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(configFile);
    runConfiguration(expc);
  }
  
  public static void runConfiguration(final ExperimentConfiguration expc) throws IOException, Exception {
    Common.currentConfiguration = expc;
    Common.currentConfiguration.framework.initFramework();
    runManager = Common.currentConfiguration.getRunManager();
    initExperiment();
    for (int i = runManager.getStartedRuns(); i < Common.currentConfiguration.runs; i = runManager.getStartedRuns()) {
      initRun(i);
      Common.currentConfiguration.framework.commenceSingleRun(i);
      finishRun();
    }
    finishExperiment();
  }
  
  public static void initRun(final int i) throws IOException {
    if (Common.currentConfiguration.debugSeed == null) {
      Common.currentSeed = Common.currentConfiguration.seeds.get(i);
    } else {
      Common.currentSeed = Common.currentConfiguration.debugSeed;
    }
    Common.currentConfiguration.experimentRandom = new Random(Common.currentSeed);
    final long churnSeed = Common.currentConfiguration.experimentRandom.nextLong() & (1L << 48) - 1;
    final long upbseed = Common.currentConfiguration.experimentRandom.nextLong() & (1L << 48) - 1;
    Utils.init();
    Common.currentConfiguration.init(churnSeed, upbseed);
    ObjectLogger.initAll();
    TextLogger.init(Common.currentSeed + "");
    runManager.initRun(i);
    System.err.println("Simulator: starting experiment");
    System.err.println("Run: " + i + ", Random seed: " + Common.currentSeed);
    System.err.println("churn seed: " + churnSeed + ", upload bandwidth seed: " + upbseed);
  }
  
  public static void finishRun() throws IOException {
    TextLogger.closeAll();
    ObjectLogger.closeAll();
    if (Common.currentConfiguration.framework.isSimulator()) {
      final LogAnalyzer logAnalyser = ObjectLogger.LA;
      logAnalyser.analyze();
      System.out.println();
      System.out.println("data for all:");
      logAnalyser.printAverageLatencyPerOrder();
      logAnalyser.printOverall();
      logAnalyser.store();
      for (final int group : ObjectLogger.grouptoLA.keySet()) {
        System.out.println("data for group: " + group);
        final LogAnalyzer groupLA = ObjectLogger.grouptoLA.get(group);
        groupLA.analyze();
        groupLA.printAverageLatencyPerOrder();
        groupLA.printOverall();
        groupLA.store();
      }
      runManager.finishRun(logAnalyser.getStoredName());
      // /TODO TEMP!!!!!!!!!!!!!!!!
      System.out.println(ChunkChoiceMeasurement.chunkChoice);
      int sum = 0;
      for (final int s : ChunkChoiceMeasurement.chunkChoice.values()) {
        sum += s;
      }
      for (final int s : ChunkChoiceMeasurement.chunkChoice.keySet()) {
        System.out.print(s + "=" + (((double) ChunkChoiceMeasurement.chunkChoice.get(s)) / sum) + ", ");
      }
      System.out.println();
    }
  }
  
  public static void initExperiment() {
    /* per experiment init */
    final File dir = new File(Common.currentConfiguration.name);
    if (!dir.exists()) {
      dir.mkdir();
    }
  }
  
  public static void finishExperiment() throws IOException {
    if (runManager.getFinishedRuns() < Common.currentConfiguration.runs) {
      return;
    }
    if (Common.currentConfiguration.framework.isSimulator()) {
      final List<String> analyzers = runManager.getAnalysers();
      if (analyzers.size() < Common.currentConfiguration.runs) {
        throw new RuntimeException("only " + analyzers.size() + "analysers created out of " + Common.currentConfiguration.runs
            + " expected");
      }
      final LogAnalyzer LA = new LogAnalyzer(analyzers, null);
      writeData(LA, null);
      for (int i = 0; i < Common.currentConfiguration.getGroupNumber() && Common.currentConfiguration.getGroupNumber() > 1; i++) {
        final LogAnalyzer groupLA = new LogAnalyzer(analyzers, i);
        writeData(groupLA, i);
      }
      if (keepResults) {
        results = LA;
      }
    }
    runManager.closeAll();
  }
  
  private static void writeData(final LogAnalyzer LA, final Integer group) {
    String addition = "All";
    if (group != null) {
      addition = "" + group;
    }
    LA.writeUploadBandwidth("uploadBandwidth" + addition);
    LA.writeAvgUploadBandwidth("uploadAvgBandwidth" + addition);
    LA.writeServerUploadBandwidth("uploadServerBandwidth" + addition);
    LA.setCSV();
    LA.writeDataUsage("dataUsage" + addition);
    LA.writeLatencyPerOrder("hopCount" + addition);
    LA.writeOverall("overall" + addition);
    LA.writeSecondInfo("secondInfo" + addition);
    LA.writePeriodInfo("periodInfo" + addition);
    LA.writechunkIdInfo("chunkIDinfo" + addition);
    LA.writeUptimeInfo("upTimeInfo" + addition);
    LA.setTSV();
    LA.writeDataUsage("dataUsage" + addition);
    LA.writeLatencyPerOrder("hopCount" + addition);
    LA.writeOverall("overall" + addition);
    LA.writeSecondInfo("secondInfo" + addition);
    LA.writePeriodInfo("periodInfo" + addition);
    LA.writechunkIdInfo("chunkIDinfo" + addition);
    LA.writeUptimeInfo("upTimeInfo" + addition);
    System.out.println();
    System.out.println("Data for group: " + addition);
    LA.printAverageLatencyPerOrder();
    // LA.printAverageDataSent();
    // LA.printSecondInfo();
    LA.printOverall();
    LA.writeConfigfile();
  }
}
