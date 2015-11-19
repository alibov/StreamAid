package experimentTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import experiment.Experiment;
import experiment.ExperimentConfiguration;
import experiment.logAnalyzer.NodeInfo;

public class TestUtils {

  static void testConfiguration(final ExperimentConfiguration expc) throws IOException, Exception {
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
    assertEquals(1.0, Experiment.results.getCI(), 0.01);
  }
}
