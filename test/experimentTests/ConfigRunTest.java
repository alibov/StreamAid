package experimentTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logging.ObjectLogger;
import logging.TextLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import peersim.config.Configuration;
import utils.Common;
import utils.Utils;
import utils.distributions.ConstantDistribution;
import experiment.Experiment;
import experiment.ExperimentConfiguration;
import experiment.logAnalyzer.NodeInfo;

@RunWith(Parameterized.class)
public class ConfigRunTest {
  String filename;
  
  public ConfigRunTest(final String filename) {
    this.filename = filename;
  }
  
  @BeforeClass public static void setUpBeforeClass() throws Exception {
    Common.currentSeed = 52622351L;
    TextLogger.disabled = true;
    TextLogger.logNodes.clear();
    ExperimentConfiguration.setDefaultBitRate(100000);
    ExperimentConfiguration.setDefaultPlaybackSeconds(40);
    ExperimentConfiguration.setDefaultRuns(2);
    ExperimentConfiguration.setDefaultUploadBandwidthDistribution(new ConstantDistribution(5560000));
  }
  
  @Parameters public static Collection<Object[]> configs() {
    final List<Object[]> retVal = new LinkedList<Object[]>();
    int skip = 0;
    for (final String exp : new File("experimentXmls").list()) {
      if (!exp.endsWith(".xml")) {
        System.err.println("WARN: non xml file in experimentXmls dir: " + exp);
        continue;
      }
      skip--;
      if (skip > 0) {
        continue;
      }
      retVal.add(new String[] { "experimentXmls" + File.separator + exp });
    }
    return retVal;
  }
  
  @Before public void setUp() throws Exception {
    Experiment.keepResults = true;
  }
  
  @After public void tearDown() throws Exception {
    ObjectLogger.closeAll();
    TextLogger.closeAll();
    // unset peersim config
    final Field field = Configuration.class.getDeclaredField("config");
    field.setAccessible(true);
    field.set(null, null);
  }
  
  @Test public void ConfigurationFilesConfigTest() throws Exception {
    System.out.println("testing configuration file: " + filename);
    ExperimentConfiguration.initDefaults();
    final ExperimentConfiguration expc = new ExperimentConfiguration(new File(filename));
    ExperimentConfiguration.initDefaults();
    final ExperimentConfiguration expc2 = new ExperimentConfiguration(expc.toXml());
    assertEquals(expc.toXml(), expc2.toXml());
    assertEquals(expc, expc2);
  }
  
  @Test public void ConfigurationFilesRunTest() throws Exception {
    System.out.println("running configuration file: " + filename);
    ExperimentConfiguration.initDefaults();
    final ExperimentConfiguration expc = new ExperimentConfiguration(new File(filename));
    testConfiguration(expc);
    Utils.deleteRecursive(new File(expc.name));
  }
  
  private static void testConfiguration(final ExperimentConfiguration expc) throws IOException, Exception {
    Experiment.runConfiguration(expc);
    assertTrue(Experiment.results.maxOrder > 0);
    final Map<String, NodeInfo> nodeInfo = Experiment.results.getNodeInfo();
    for (final NodeInfo ni : nodeInfo.values()) {
      assertTrue(ni.bytesSent > 0);
      assertTrue(ni.messagesSent > 0);
      if (ni.chunkInfo.isEmpty()) {
        assertTrue(ni.leaveTime - ni.joinTime < 20000);
      }
    }
    // assertEquals(1.0, Experiment.results.getCI(), 0.01);
  }
}
