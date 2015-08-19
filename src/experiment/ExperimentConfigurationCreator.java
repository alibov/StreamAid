package experiment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import logging.ObjectLogger;
import utils.Utils;
import utils.distributions.ConstantDistribution;
import utils.distributions.LogNormalDistribution;
import experiment.ChurnModel.Type;
import experiment.frameworks.PeerSimFramework;

public class ExperimentConfigurationCreator {
  public static void main(final String[] args) throws IOException {
    final String suiteName = "Tables10InitLatency10";
    final File suiteDir = new File(suiteName);
    if (suiteDir.exists()) {
      Utils.deleteRecursive(suiteDir);
    }
    suiteDir.mkdir();
    final int netWorkSizes[] = { 300 };
    final int sturtupBufferLengths[] = { 2000 };
    final boolean bufferFromFirstchunk[] = { true, false };
    final boolean WaitIfNoChunk[] = { true, false };
    // final int skipLookahead[] = { 1, 2 };
    final int inits[] = { 10 };
    // final int buffers[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    final int buffers[] = { 0 };
    final int initOffsets[] = { 0 };
    final double alphas[] = { 0.75 };
    // final String overlays[] = { getAraneolaXML(4, 12, ""),
    final String overlays[] = { getAraneolaXML(3, 12, ""), getAraneolaXML(4, 12, ""), getCoolstreamingOverlayXML(1, 4, 10, 6) };
    final int bitrates[] = { 300000 };
    final List<String> algs = new LinkedList<String>();
    final List<String> algRes = new LinkedList<String>();
    final ChurnModel cm = new ChurnModel(Type.sessionLengthAddOnFailure);
    cm.setSessionLengthDistribution(new LogNormalDistribution(4.29, 1.28));
    ExperimentConfiguration.setDefaultChurnModel(cm);
    ExperimentConfiguration.setDefaultFramework(new PeerSimFramework(0.0, 200, 400));
    ExperimentConfiguration.setDefaultPlaybackSeconds(300);
    ExperimentConfiguration.setDefaultRuns(10);
    ExperimentConfiguration.setDefaultName(suiteName);
    ExperimentConfiguration.setDefaultUploadBandwidthDistribution(new ConstantDistribution(5560000));
    ExperimentConfiguration.setDefaultServerBandwidth(3 * 5560000);
    int count = 0;
    final FileOutputStream fileliststream = new FileOutputStream(suiteName + File.separator + "fileList.list");
    for (final int networkSize : netWorkSizes) {
      for (final int startupBufferLength : sturtupBufferLengths) {
        for (final int bitrate : bitrates) {
          // ExperimentConfiguration.setDefaultStartupBuffering(startupBufferLength);
          ExperimentConfiguration.setDefaultBitRate(bitrate);
          for (final boolean bf : bufferFromFirstchunk) {
            for (final boolean winc : WaitIfNoChunk) {
              // ExperimentConfiguration.setDefaultBufferFromFirstChunk(bf);
              // ExperimentConfiguration.setDefaultWaitIfNoChunk(winc);
              algs.clear();
              algRes.clear();
              for (final String overlay : overlays) {
                for (final int initOffset : initOffsets) {
                  for (final int init : inits) {
                    for (final int buffer : buffers) {
                      for (final double alpha : alphas) {
                        algs.add(getExperiment(getCoolstreamingXML(overlay, "", networkSize, true, initOffset, 10)));
                        algs.add(getExperiment(getCoolstreamingXML(overlay, "", networkSize, false, initOffset, 10)));
                      }
                    }
                  }
                }
              }
              for (final String algXML : algs) {
                try {
                  final ExperimentConfiguration conf = new ExperimentConfiguration(algXML);
                  // conf.getStreamingAlg.get(0).getName();
                  FileOutputStream fstream;
                  fstream = new FileOutputStream(suiteName + File.separator + conf.toString() + ".xml");
                  fstream.write(conf.toXml().getBytes());
                  fileliststream.write((conf.toString() + ".xml\n").getBytes());
                  count++;
                  fstream.close();
                  fstream = null;
                } catch (final Exception e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    }
    fileliststream.close();
    ObjectLogger.closeAll();
    System.out.println("created " + count + " experiments!");
  }
  
  public static String getExperiment(final String experiment) {
    return "<experiment>\n" + experiment + "\n</experiment>\n";
  }
  
  public static String getCoolstreamingXML(final String overlay, final String behaviors, final int networkSize,
      final boolean sourcePush, final int maxInitOffset, final int maxInitLatency) {
    return "<streamingAlgorithm algorithm=\"CoolStreaming\" maxInitOffset =\"" + maxInitOffset + "\" maxInitLatency =\""
        + maxInitLatency + "\" sourcePush=\"" + sourcePush + "\" size=\"" + networkSize + "\">\n" + overlay + "\n" + behaviors
        + "\n" + "</streamingAlgorithm>";
  }
  
  public static String getAraneolaXML(final int L, final int S, final String behaviors) {
    return " <overlayAlgorithm algorithm=\"AraneolaOverlay\" S=\"" + S + "\" gossipDelay=\"6\" \n" + "amountToSend=\"3\" L=\"" + L
        + "\" H=\"10\" connect_timeout=\"2\" disconnect_timeout=\"2\">\n" + "<clusteringAlgorithm algorithm=\"FullCluster\" />\n"
        + behaviors + "\n" + "</overlayAlgorithm>";
  }
  
  public static String getDNVPBehavior() {
    return "<behavior name=\"DNVPOverlayBehavior\" timeoutcycle=\"5\" expirationInterval=\"20\" numOfNoncesToProduce=\"20\"\n"
        + "checkApproval=\"1.0\" checkDisconnection=\"1.0\" verificationsPerCycle = \"2\"/>";
  }
  
  public static String getBlackListBehavior() {
    return "<behavior name=\"BlackListBehavior\"/>";
  }
  
  public static String getLookaheadSkipPolicyBehavior(final int lookAhead) {
    return "<behavior name=\"LookaheadSkipPolicyBehavior\" numberOfLookaheadChunks=\"" + lookAhead + "\" />\n";
  }
  
  public static String getAdaptiveAdaptivePlayoutBehavior(final int bufferSize, final double maxSpeedChange, final int initRounds,
      final double alpha) {
    return "<behavior name=\"AdaptiveAdaptivePlayoutBehavior\" bufferSize=\"" + bufferSize + "\" maxSpeedChange=\""
        + maxSpeedChange + "\" initRounds=\"" + initRounds + "\" alpha=\"" + alpha + "\" />\n";
  }
  
  public static String getAuditingBehavior() {
    return "<behavior name=\"AuditingBehavior\" thresholdForSource=\"5\" thresholdForCommittee=\"1\"/>";
  }
  
  public static String getOmissionDefenseBehavior() {
    return "<behavior name = \"OmissionDefenceBehavior2\" numOfCycles=\"10\" thresholdForMisses=\"1\" roundsForBitmap=\"2\" roundsForAnswer=\"1\" verificationsPerCycle=\"2\"/>";
  }
  
  public static String getChunksOmissionBehavior(final double dropRate) {
    return "<behavior name=\"ChunksOmissionBehavior\" dropRate=\"" + dropRate + "\"/>";
  }
  
  public static String getBitmapOmissionBehavior(final double dropRate) {
    return "<behavior name=\"BitmapOmissionBehavior\" dropRate=\"" + dropRate + "\"/>";
  }
  
  public static String getMtreeboneSCAMPXML(final int i, final int networkSize) {
    return getMtreeboneXML(getSCAMPoverlay(i), 0.3, networkSize);
  }
  
  public static String getInnerCoolstreaming(final boolean sourcePush, final String overlay, final String behaviors) {
    return "<streamingAlgorithm algorithm=\"CoolStreaming\" sourcePush=\"" + sourcePush + "\">\n" + overlay + "\n" + behaviors
        + "</streamingAlgorithm> \n";
  }
  
  public static String getConstantBufferSizeAdaptivePlayoutBehavior(final int bufferSize, final double maxSpeedChange) {
    return "\t<behavior name=\"ConstantBufferSizeAdaptivePlayoutBehavior\" bufferSize=\"" + bufferSize + "\" maxSpeedChange=\""
        + maxSpeedChange + "\" />";
  }
  
  public static String getTreeBoneOverlay(final double stableCoeff, final int queryTimeout, final int freeSlots) {
    return "\t<overlayAlgorithm algorithm=\"TreeBoneOverlay\" stableCoefficient=\"" + stableCoeff + "\" queryTimeout=\""
        + queryTimeout + "\" freeSlots=\"" + freeSlots + "\" />";
  }
  
  public static String getPushPullXML(final String streaming, final String overlay, final int networkSize) {
    return "<experiment>" + "\t<streamingAlgorithm algorithm=\"PushPullAlgorithm\"  size=\"" + networkSize + "\">\n" + streaming
        + "\n" + overlay + "\n" + "\t</streamingAlgorithm> \n" + "</experiment>\n";
  }
  
  public static String getMtreeboneXML(final String overlay, final double stableCoeff, final int networkSize) {
    return "<experiment>" + "<streamingAlgorithm algorithm=\"MtreeBone\"  size=\"" + networkSize + "\">\n"
        + "\t\t<streamingAlgorithm algorithm=\"PushTree\">\n"
        + "\t\t<overlayAlgorithm algorithm=\"TreeBoneOverlay\" stableCoefficient=\"" + stableCoeff + "\" queryTimeout=\"3\">\n"
        + "\t\t\t<clusteringAlgorithm algorithm=\"FullCluster\" />\n" + "\t\t</overlayAlgorithm>\n" + "\t\t</streamingAlgorithm>\n"
        + overlay + "\t</streamingAlgorithm> \n" + "</experiment>\n";
  }
  
  public static String getSCAMPoverlay(final int i) {
    return "<overlayAlgorithm algorithm=\"SCAMPOverlay\" c=\"" + i + "\">\n"
        + "	<clusteringAlgorithm algorithm=\"FullCluster\" />\n" + "</overlayAlgorithm>\n";
  }
  
  public static String getBSCAMPoverlay(final int i) {
    return "<overlayAlgorithm algorithm=\"BSCAMPOverlay\" c=\"" + i + "\">\n"
        + " <clusteringAlgorithm algorithm=\"FullCluster\" />\n" + "</overlayAlgorithm>\n";
  }
  
  public static String getMtreeboneSCAMPXML(final int networkSize) {
    return getMtreeboneSCAMPXML(3, networkSize);
  }
  
  public static String getMtreeboneAraneolaXML(final int networkSize) {
    return getMtreeboneAraneolaXML(3, 12, networkSize);
  }
  
  public static String getMtreeboneAraneolaXML(final int L, final int S, final int networkSize) {
    return getMtreeboneXML(getAraneolaOverlay(L, S), 0.3, networkSize);
  }
  
  public static String getCoolstreamingAraneolaXML(final int L, final int S, final int networkSize) {
    return "<experiment>" + "<streamingAlgorithm algorithm=\"CoolStreaming\"  size=\"" + networkSize + "\">\n"
        + getAraneolaOverlay(L, S) + "</streamingAlgorithm> \n" + "</experiment>\n";
  }
  
  public static String getAraneolaOverlay(final int L, final int S) {
    return " <overlayAlgorithm algorithm=\"AraneolaOverlay\" S=\"" + S + "\" gossipDelay=\"6\" \n" + "amountToSend=\"3\" L=\"" + L
        + "\" H=\"10\" connect_timeout=\"2\" disconnect_timeout=\"2\">\n" + "<clusteringAlgorithm algorithm=\"FullCluster\" />\n"
        + "</overlayAlgorithm>\n";
  }
  
  public static String getCoolstreamingAraneolaXML(final int networkSize) {
    return getCoolstreamingAraneolaXML(3, 12, networkSize);
  }
  
  public static String getCoolstreamingSCAMPXML(final int networkSize) {
    return getCoolstreamingSCAMPXML(3, networkSize);
  }
  
  public static String getCoolstreamingSCAMPXML(final int i, final int networkSize) {
    return "<experiment>" + "<streamingAlgorithm algorithm=\"CoolStreaming\" size=\"" + networkSize + "\">\n" + getSCAMPoverlay(i)
        + "</streamingAlgorithm> \n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingXML(final String overlay, final int networkSize) {
    return "<experiment>" + "<streamingAlgorithm algorithm=\"CoolStreaming\" size=\"" + networkSize + "\">\n" + overlay
        + "</streamingAlgorithm> \n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingWithOmissionXML(final String overlay, final int networkSize,
      final double percentageOfMalicious) {
    final int maliciousNodes = (int) (networkSize * percentageOfMalicious);
    final int legitimateNodes = networkSize - maliciousNodes;
    return "<experiment>" + "<streamingAlgorithm algorithm=\"CoolStreaming\" size=\"" + legitimateNodes + "\">\n" + overlay
        + "</streamingAlgorithm> \n" + "<streamingAlgorithm algorithm=\"CoolStreaming\" size=\"" + maliciousNodes + "\">\n"
        + overlay + "<behavior name=\"OmissionAttackBehavior\"/>\n</streamingAlgorithm>\n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingOverlayXML(final int c, final int M, final int H, final int amountToSend) {
    return "<overlayAlgorithm H=\"" + H + "\" M=\"" + M + "\" c=\"" + c + "\" algorithm=\"CoolStreamingOverlay\" amountToSend=\""
        + amountToSend + "\" exploreRound=\"30\" gossipTimeout=\"6\">\n" + " <clusteringAlgorithm algorithm=\"FullCluster\" />\n"
        + "</overlayAlgorithm>\n";
  }
  
  public static String getPrimeOverlayXML(final int groupSize) {
    return "<overlayAlgorithm algorithm=\"PrimeOverlay\" size=\"" + groupSize + "\">\n"
        + "	<clusteringAlgorithm algorithm=\"FullCluster\" />\n" + "</overlayAlgorithm>\n";
  }
}
