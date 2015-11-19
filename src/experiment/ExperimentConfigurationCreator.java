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
    return "<experiment>\n" + getDefaultPlayerModule() + experiment + "\n</experiment>\n";
  }
  
  public static String getCoolstreamingXML(final String overlay, final String ingredients, final int networkSize,
      final boolean sourcePush, final int maxInitOffset, final int maxInitLatency) {
    return "<streamingModule algorithm=\"CoolStreaming\" maxInitOffset =\"" + maxInitOffset + "\" maxInitLatency =\""
        + maxInitLatency + "\" sourcePush=\"" + sourcePush + "\" size=\"" + networkSize + "\">\n" + overlay + "\n" + ingredients
        + "\n" + "</streamingModule>";
  }
  
  public static String getAraneolaXML(final int L, final int S, final String ingredients) {
    return " <overlayModule algorithm=\"Araneola\" S=\"" + S + "\" gossipDelay=\"6\" \n" + "amountToSend=\"3\" L=\"" + L
        + "\" H=\"10\" connect_timeout=\"2\" disconnect_timeout=\"2\">\n" + ingredients + "\n" + "</overlayModule>";
  }
  
  public static String getDNVPIngredient() {
    return "<ingredient name=\"DNVPOverlayIngredient\" timeoutcycle=\"5\" expirationInterval=\"20\" numOfNoncesToProduce=\"20\"\n"
        + "checkApproval=\"1.0\" checkDisconnection=\"1.0\" verificationsPerCycle = \"2\"/>";
  }
  
  public static String getBlackListIngredient() {
    return "<ingredient name=\"BlackListIngredient\"/>";
  }
  
  public static String getLookaheadSkipPolicyIngredient(final int lookAhead) {
    return "<ingredient name=\"LookaheadSkipPolicyIngredient\" numberOfLookaheadChunks=\"" + lookAhead + "\" />\n";
  }
  
  public static String getAdaptiveAdaptivePlayoutIngredient(final int bufferSize, final double maxSpeedChange,
      final int initRounds, final double alpha) {
    return "<ingredient name=\"AdaptiveAdaptivePlayoutIngredient\" bufferSize=\"" + bufferSize + "\" maxSpeedChange=\""
        + maxSpeedChange + "\" initRounds=\"" + initRounds + "\" alpha=\"" + alpha + "\" />\n";
  }
  
  public static String getAuditingIngredient() {
    return "<ingredient name=\"AuditingIngredient\" thresholdForSource=\"5\" thresholdForCommittee=\"1\"/>";
  }
  
  public static String getOmissionDefenseIngredient() {
    return "<ingredient name = \"OmissionDefenceIngredient2\" numOfCycles=\"10\" thresholdForMisses=\"1\" roundsForBitmap=\"2\" roundsForAnswer=\"1\" verificationsPerCycle=\"2\"/>";
  }
  
  public static String getChunksOmissionIngredient(final double dropRate) {
    return "<ingredient name=\"ChunksOmissionIngredient\" dropRate=\"" + dropRate + "\"/>";
  }
  
  public static String getBitmapOmissionIngredient(final double dropRate) {
    return "<ingredient name=\"BitmapOmissionIngredient\" dropRate=\"" + dropRate + "\"/>";
  }
  
  public static String getPushPullSCAMPXML(final int i, final int networkSize) {
    return getPushPullXML(getSCAMPoverlay(i), 0.3, 1, networkSize);
  }
  
  public static String getInnerCoolstreaming(final boolean sourcePush, final String overlay, final String ingredients) {
    return "<streamingModule algorithm=\"CoolStreaming\" sourcePush=\"" + sourcePush + "\">\n" + overlay + "\n" + ingredients
        + "</streamingModule> \n";
  }
  
  public static String getConstantBufferSizeAdaptivePlayoutIngredient(final int bufferSize, final double maxSpeedChange) {
    return "\t<ingredient name=\"ConstantBufferSizeAdaptivePlayoutIngredient\" bufferSize=\"" + bufferSize + "\" maxSpeedChange=\""
        + maxSpeedChange + "\" />";
  }
  
  public static String getPushPull(final double stableCoeff, final int queryTimeout, final int freeSlots) {
    return "\t<overlayModule algorithm=\"PushPull\" stableCoefficient=\"" + stableCoeff + "\" queryTimeout=\"" + queryTimeout
        + "\" freeSlots=\"" + freeSlots + "\" />";
  }
  
  public static String getPushPullXML(final String streaming, final String overlay, final int networkSize) {
    return "<experiment>" + getDefaultPlayerModule() + "\t<streamingModule algorithm=\"PushPull\"  size=\"" + networkSize + "\">\n"
        + streaming + "\n" + overlay + "\n" + "\t</streamingModule> \n" + "</experiment>\n";
  }
  
  public static String getPushPullXML(final String overlay, final double stableCoeff, final int freeSlots, final int networkSize) {
    return "<experiment> " + getDefaultPlayerModule() + " <streamingModule algorithm=\"PushPull\"  size=\"" + networkSize + "\">\n"
        + "\t\t<overlayModule algorithm=\"TreeBone\" stableCoefficient=\"" + stableCoeff + "\" queryTimeout=\"3\" freeSlots=\""
        + freeSlots + "\" />\n" + "\t\t<streamingModule algorithm=\"CoolStreaming\">\n" + overlay + "\t\t</streamingModule> \n"
        + "\t</streamingModule> \n" + "</experiment>\n";
  }
  
  private static String getDefaultPlayerModule() {
    return "<playerModule waitIfNoChunk=\"\" bufferFromFirstChunk=\"true\""
        + " serverStartup=\"10000\" startupBuffering=\"2000\" streamWindowSize=\"120\" />";
  }
  
  public static String getSCAMPoverlay(final int i) {
    return "<overlayModule algorithm=\"SCAMP\" c=\"" + i + "\" />";
  }
  
  public static String getBSCAMPoverlay(final int i) {
    return "<overlayModule algorithm=\"BSCAMP\" c=\"" + i + "\" />";
  }
  
  public static String getPushPullSCAMPXML(final int networkSize) {
    return getPushPullSCAMPXML(3, networkSize);
  }
  
  public static String getPushPullAraneolaXML(final int networkSize) {
    return getPushPullAraneolaXML(3, 12, networkSize);
  }
  
  public static String getPushPullAraneolaXML(final int L, final int S, final int networkSize) {
    return getPushPullXML(getAraneola(L, S), 0.3, 1, networkSize);
  }
  
  public static String getCoolstreamingAraneolaXML(final int L, final int S, final int networkSize) {
    return "<experiment>" + getDefaultPlayerModule() + "<streamingModule algorithm=\"CoolStreaming\"  size=\"" + networkSize
        + "\">\n" + getAraneola(L, S) + "</streamingModule> \n" + "</experiment>\n";
  }
  
  public static String getAraneola(final int L, final int S) {
    return " <overlayModule algorithm=\"Araneola\" S=\"" + S + "\" gossipDelay=\"6\" \n" + "amountToSend=\"3\" L=\"" + L
        + "\" H=\"10\" connect_timeout=\"2\" disconnect_timeout=\"2\" />";
  }
  
  public static String getCoolstreamingAraneolaXML(final int networkSize) {
    return getCoolstreamingAraneolaXML(3, 12, networkSize);
  }
  
  public static String getCoolstreamingSCAMPXML(final int networkSize) {
    return getCoolstreamingSCAMPXML(3, networkSize);
  }
  
  public static String getCoolstreamingSCAMPXML(final int i, final int networkSize) {
    return "<experiment>" + getDefaultPlayerModule() + "<streamingModule algorithm=\"CoolStreaming\" size=\"" + networkSize
        + "\">\n" + getSCAMPoverlay(i) + "</streamingModule> \n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingXML(final String overlay, final int networkSize) {
    return "<experiment>" + getDefaultPlayerModule() + "<streamingModule algorithm=\"CoolStreaming\" size=\"" + networkSize
        + "\">\n" + overlay + "</streamingModule> \n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingWithOmissionXML(final String overlay, final int networkSize,
      final double percentageOfMalicious) {
    final int maliciousNodes = (int) (networkSize * percentageOfMalicious);
    final int legitimateNodes = networkSize - maliciousNodes;
    return "<experiment>" + getDefaultPlayerModule() + "<streamingModule algorithm=\"CoolStreaming\" size=\"" + legitimateNodes
        + "\">\n" + overlay + "</streamingModule> \n" + "<streamingModule algorithm=\"CoolStreaming\" size=\"" + maliciousNodes
        + "\">\n" + overlay + "<ingredient name=\"OmissionAttackIngredient\"/>\n</streamingModule>\n" + "</experiment>\n";
  }
  
  public static String getCoolstreamingOverlayXML(final int c, final int M, final int H, final int amountToSend) {
    return "<overlayModule H=\"" + H + "\" M=\"" + M + "\" c=\"" + c + "\" algorithm=\"CoolStreamingOverlay\" amountToSend=\""
        + amountToSend + "\" exploreRound=\"30\" gossipTimeout=\"6\" />";
  }
  
  public static String getPrimeOverlayXML(final int groupSize) {
    return "<overlayModule algorithm=\"PrimeOverlay\" size=\"" + groupSize + "\" />";
  }
}
