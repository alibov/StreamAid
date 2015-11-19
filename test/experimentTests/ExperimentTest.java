package experimentTests;

import java.io.File;
import java.lang.reflect.Field;

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

public class ExperimentTest {
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
  }
  
  @AfterClass public static void tearDownAfterClass() throws Exception {
    if (new File(expNameString).exists()) {
      Utils.deleteRecursive(new File(expNameString));
    }
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
    TestUtils.testConfiguration(expc);
  }
  
  @Test public void CoolStreamingAraneolaTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(
        ExperimentConfigurationCreator.getCoolstreamingAraneolaXML(testSize));
    TestUtils.testConfiguration(expc);
  }
  
  @Test public void CoolStreamingPrimeTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(ExperimentConfigurationCreator.getCoolstreamingXML(
        ExperimentConfigurationCreator.getPrimeOverlayXML(6), testSize));
    TestUtils.testConfiguration(expc);
  }
  
  @Test public void mTreeBoneAraneolaTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(
        ExperimentConfigurationCreator.getPushPullAraneolaXML(testSize));
    TestUtils.testConfiguration(expc);
  }
  
  @Test public void mTreeBoneSCAMPTest() throws Exception {
    final ExperimentConfiguration expc = new ExperimentConfiguration(ExperimentConfigurationCreator.getPushPullSCAMPXML(testSize));
    TestUtils.testConfiguration(expc);
  }
}
