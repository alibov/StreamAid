package experimentTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logging.ObjectLogger;
import logging.TextLogger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import peersim.config.Configuration;
import utils.Common;
import utils.Utils;
import utils.distributions.ConstantDistribution;
import experiment.Experiment;
import experiment.ExperimentConfiguration;
import experiment.ExperimentConfigurationCreator;
import experiment.logAnalyzer.NodeInfo;

public class ExperimentTest {
  public final static List<String> confFiles = new LinkedList<String>();
  public final static String expNameString = "testName";
  public final static int testSize = 20;
  
  @BeforeClass public static void setUpBeforeClass() throws Exception {
    Common.currentSeed = 52622351L;
    TextLogger.disabled = true;
    TextLogger.logNodes.clear();
    ExperimentConfiguration.setDefaultBitRate(100000);
    ExperimentConfiguration.setDefaultName(expNameString);
    ExperimentConfiguration.setDefaultPlaybackSeconds(40);
    ExperimentConfiguration.setDefaultRuns(2);
    ExperimentConfiguration.setDefaultUploadBandwidthDistribution(new ConstantDistribution(5560000));
    confFiles.add("coolstreaming.xml");
    confFiles.add("coolstreamingAraneola.xml");
    confFiles.add("coolstreamingPrime.xml");
    confFiles.add("coolstreamingSCAMP.xml");
    confFiles.add("file.xml");
    confFiles.add("Prime.xml");
    confFiles.add("SopCast.xml");
  }
  
  @AfterClass public static void tearDownAfterClass() throws Exception {
    Utils.deleteRecursive(new File("testName"));
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
  
  @Test public void CoolStreamingSCAMPTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(
        ExperimentConfigurationCreator.getCoolstreamingSCAMPXML(testSize));
    testConfiguration(expc);
  }
  
  @Test public void CoolStreamingAraneolaTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(
        ExperimentConfigurationCreator.getCoolstreamingAraneolaXML(testSize));
    testConfiguration(expc);
  }
  
  @Test public void CoolStreamingPrimeTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(ExperimentConfigurationCreator.getCoolstreamingXML(
        ExperimentConfigurationCreator.getPrimeOverlayXML(6), testSize));
    testConfiguration(expc);
  }
  
  @Test public void mTreeBoneAraneolaTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(
        ExperimentConfigurationCreator.getMtreeboneAraneolaXML(testSize));
    testConfiguration(expc);
  }
  
  @Test public void mTreeBoneSCAMPTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(ExperimentConfigurationCreator.getMtreeboneSCAMPXML(testSize));
    testConfiguration(expc);
  }
  
  @Test public void ConfigurationFilesTest() throws Exception {
    for (final String filename : confFiles) {
      System.out.println("testing configuration file: " + filename);
      final ExperimentConfiguration expc = new ExperimentConfiguration(new File(filename));
      final ExperimentConfiguration expc2 = new ExperimentConfiguration(expc.toXml());
      assertEquals(expc.toXml(), expc2.toXml());
      assertEquals(expc, expc2);
    }
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
    assertEquals(1.0, Experiment.results.getCI(), 0.0001);
  }
}
