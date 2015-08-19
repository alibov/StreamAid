package utils;

import java.util.LinkedList;
import java.util.List;

import experiment.ExperimentConfiguration;

/**
 * Holds common configuration. Also, current experiment configuration and
 * framework.
 * 
 * @author Alexander Libov
 * 
 */
public class Common {
  public static long seed = 12345678L;
  public static long currentSeed;
  static {
    // TextLogger.logNodes.add("127.0.0.1-8007");
    // TextLogger.logNodes.add("431");
  }
  public static List<ExperimentConfiguration> configurations = new LinkedList<ExperimentConfiguration>();
  public static ExperimentConfiguration currentConfiguration;
}
