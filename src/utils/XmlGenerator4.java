package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import experiment.ExperimentConfiguration;

public class XmlGenerator4 {
  static final int playbackSeconds = 2400;
  static final int runs = 5;
  static final String dir = "MABAlgoTest7";
  static final String chunkAvailability = "<ingredient name=\"NeighborChunkAvailabilityIngredient\" operationMode=\"UpdateEveryChunk\"/>";
  static final String[] overlays = {
      "<overlayModule H=\"200\" M=\"4\" c=\"1\" amountToSend=\"3\" exploreRound=\"30\" gossipTimeout=\"6\" algorithm=\"CoolStreamingOverlay\">"
          + chunkAvailability + "</overlayModule>",
      "<overlayModule H=\"200\" M=\"7\" c=\"1\" amountToSend=\"3\" exploreRound=\"30\" gossipTimeout=\"6\" algorithm=\"CoolStreamingOverlay\">"
          + chunkAvailability + "</overlayModule>",
      "<overlayModule groupSize=\"4\" algorithm=\"RandomMinSizeGroupOverlay\" >" + chunkAvailability + "</overlayModule>",
      "<overlayModule H=\"10\" L=\"4\" S=\"12\" algorithm=\"Araneola\" amountToSend=\"3\" connect_timeout=\"2\" disconnect_timeout=\"2\" gossipDelay=\"6\">"
          + chunkAvailability + "</overlayModule>",
      "<overlayModule H=\"10\" L=\"7\" S=\"12\" algorithm=\"Araneola\" amountToSend=\"3\" connect_timeout=\"2\" disconnect_timeout=\"2\" gossipDelay=\"6\">"
          + chunkAvailability + "</overlayModule>",
      "<overlayModule algorithm=\"BSCAMP\" c=\"1\">" + chunkAvailability + "</overlayModule>" };
  static final String[] overlayName = { "Coolstreaming4", "Coolstreaming7", "RandomMinSize", "Araneola4", "Araneola7", "BSCAMP" };
  static final String[] uploadBandwidth = {
      "<serverUploadBandwidth value=\"20000000\" />\n"
          + "<uploadBandwidthDistribution> <distribution type=\"NormalDistribution\" mean=\"12600000\" variance=\"250000000000\" />  </uploadBandwidthDistribution>",
      "<serverUploadBandwidth value=\"20000000\" />\n"
          + "<uploadBandwidthDistribution> <distribution type=\"NormalDistribution\" mean=\"2600000\" variance=\"250000000000\" />  </uploadBandwidthDistribution>" };
  static final String[] churn = {
      "<churnModel type=\"sessionLengthAddOnFailure\"><sessionLengthDistribution><distribution type=\"LogNormalDistribution\" "
          + "mean=\"6.29\" variance=\"1.28\" /> </sessionLengthDistribution>  </churnModel>\n",
      "<churnModel type=\"sessionLengthAddOnFailure\"><sessionLengthDistribution><distribution type=\"LogNormalDistribution\" "
          + "mean=\"3.29\" variance=\"1.28\" /> </sessionLengthDistribution>  </churnModel>\n" };
  static final String[] player = { "<playerModule waitIfNoChunk=\"\" bufferFromFirstChunk=\"true\" serverStartup=\"10000\" startupBuffering=\"2000\" streamWindowSize=\"120\" />" };
  static final String[] playerName = { "wait2" };
  static final String[] networkSize = { "200" };
  static final String[] chunkRequest = { "<ingredient name=\"ChainsawChunkRequestIngredient\"/>",
      "<ingredient name=\"CoolstreamingChunkRequestIngredient\"/>" };
  static final String[] chunkRequestName = { "ChainSaw", "Coolstreaming" };
  static final String banditsSuffix = "<banditReward name=\"InfoRepTreeGather\" timeout=\"10\">"
      + "<banditReward name=\"ChunksReceivedReward\" />" + "<banditReward name=\"ChunksReceivedAvailableReward\" />"
      + "<banditReward name=\"SimpleCIReward\" />" + "</banditReward></banditsAlgo>";
  /* static final String[] bandits = {
   * "<banditsAlgo name=\"EpsilonGreedy\" constant=\"0.2\" >" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" constant=\"0.25\" >" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" linear=\"0.1\">" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" linear=\"0.05\">" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"100\">" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"200\">" + banditsSuffix,
   * "<banditsAlgo name=\"UCB1\" conv=\"0\">" + banditsSuffix,
   * "<banditsAlgo name=\"UCB1\" conv=\"10\">" + banditsSuffix,
   * "<banditsAlgo name=\"UCB1\" conv=\"100\">" + banditsSuffix }; static final
   * String[] banditsName = { "EpsilonGreedy_C0.2", "EpsilonGreedy_C0.25",
   * "EpsilonGreedy_L0.1", "EpsilonGreedy_L0.05", "EpsilonGreedy_IL100",
   * "EpsilonGreedy_IL200", "UCB1_0", "UCB1_10", "UCB1_100" }; */
  /* static final String[] bandits = {
   * "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"2\">" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"3\">" + banditsSuffix,
   * "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"4\">" + banditsSuffix,
   * "<banditsAlgo name=\"UCB1\" conv=\"50\">" + banditsSuffix }; static final
   * String[] banditsName = { "EpsilonGreedy_IL2", "EpsilonGreedy_IL3",
   * "EpsilonGreedy_IL4", "UCB1_50" }; */
  /* static final String[] bandits = {
   * "<banditsAlgo name=\"UCB1\" conv=\"150\">" + banditsSuffix,
   * "<banditsAlgo name=\"UCB1\" conv=\"200\">" + banditsSuffix }; static final
   * String[] banditsName = { "UCB1_150", "UCB1_200" }; */
  static final String[] bandits = { "<banditsAlgo name=\"EpsilonGreedy\" constant=\"0.2\" >" + banditsSuffix,
    "<banditsAlgo name=\"EpsilonGreedy\" linear=\"0.1\">" + banditsSuffix,
    "<banditsAlgo name=\"EpsilonGreedy\" invlinear=\"4\">" + banditsSuffix,
    "<banditsAlgo name=\"UCB1\" conv=\"150\">" + banditsSuffix };
  static final String[] banditsName = { "EG_C0.2", "EG_L0.1", "EG_IL4", "UCB1_150" };
  static final String[] changeTimeout = { "100" };
  
  static void generateSingleXml(final PrintWriter fileliststream, final String name, final int uploadSetting,
      final int churnSetting, final int playerSetting, final String streamingModule) throws FileNotFoundException {
    final StringBuffer sb = new StringBuffer();
    sb.append("<experiment>\n" + "<name value=\"" + name + "\" />\n" + "<seed value=\"12345678\" />\n"
        + "<playbackSeconds value=\"" + playbackSeconds + "\" />\n" + "<runs value=\"" + runs + "\" />\n"
        + "<messageHeaderSize value=\"64\" />\n" + "<cycleLength value=\"1000\" />\n"
        + "<framework value=\"PeerSimFramework\" dropRate=\"0.0\" />\n" + "<bitrate value=\"500000\" />\n"
        + "<descriptions value=\"6\" />\n<enableWaitQueueLimit />\n");
    sb.append(uploadBandwidth[uploadSetting]);
    sb.append(churn[churnSetting]);
    sb.append(player[playerSetting]);
    sb.append(streamingModule);
    sb.append("</experiment>");
    final PrintWriter out = new PrintWriter(dir + File.separator + name + ".xml");
    out.print(sb.toString());
    out.close();
    fileliststream.print(name + ".xml\n");
    // sanity check!
    try {
      // System.out.println(sb.toString());
      final ExperimentConfiguration conf1 = new ExperimentConfiguration(sb.toString());
      final ExperimentConfiguration conf2 = new ExperimentConfiguration(conf1.toXml());
      if (!conf1.equals(conf2)) {
        throw new IllegalStateException("confs are different!");
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  // ch0u0-wait2-ChainSaw-Araneola7-pushpull
  // ch0u1-wait2-Coolstreaming-RandomMinSize-pushpull
  // ch1u0-wait2-ChainSaw-Coolstreaming7-pull.xml.outputLog
  // ch1u0-wait2-Coolstreaming-Araneola7-pull.xml.outputLog
  // ch1u1-wait2-ChainSaw-RandomMinSize-pushpull.xml.outputLog
  public static void main(final String[] args) throws FileNotFoundException {
    if (!new File(dir).exists()) {
      new File(dir).mkdirs();
    }
    // 0 - chaisaw, 1- CS
    final int[] bestChunkRequestSetting = new int[] { 0, 1, 0, 0, 1 };
    // "Coolstreaming4", "Coolstreaming7", "RandomMinSize", "Araneola4",
    // "Araneola7", "BSCAMP"
    final int[] bestOverlaySetting = new int[] { 4, 2, 1, 2, 4 };
    final boolean[] bestpushpull = new boolean[] { true, true, false, true, false };
    String all = "";
    for (int i = 0; i < 4; ++i) {
      all += bestpushpull[i] ? generatePushPull(bestChunkRequestSetting[i], bestOverlaySetting[i], "1000") : generatePull(
          bestChunkRequestSetting[i], bestOverlaySetting[i], "1000");
    }
    String two = "";
    for (int i = 1; i < 3; ++i) {
      two += bestpushpull[i] ? generatePushPull(bestChunkRequestSetting[i], bestOverlaySetting[i], "1000") : generatePull(
          bestChunkRequestSetting[i], bestOverlaySetting[i], "1000");
    }
    final PrintWriter fileliststream = new PrintWriter(dir + File.separator + "fileList.list");
    for (int churnSetting = 0; churnSetting < churn.length; ++churnSetting) {
      for (final String nodes : networkSize) {
        for (int uploadSetting = 0; uploadSetting < uploadBandwidth.length; ++uploadSetting) {
          final String name = "ch" + churnSetting + "u" + uploadSetting;
          final int playerSetting = 0;
          for (int bSetting = 0; bSetting < bandits.length; ++bSetting) {
            /* generateSingleXml(fileliststream, name + "-" +
             * playerName[playerSetting] + "-MAB4-" + banditsName[bSetting],
             * uploadSetting, churnSetting, playerSetting,
             * "<streamingModule algorithm=\"AdaptiveStreaming\" size=\"" +
             * nodes + "\" changeTimeout=\"100\">" + all + bandits[bSetting] +
             * "</streamingModule>"); */
            generateSingleXml(fileliststream, name + "-" + playerName[playerSetting] + "-MAB2-" + banditsName[bSetting],
                uploadSetting, churnSetting, playerSetting, "<streamingModule algorithm=\"AdaptiveStreaming\" size=\"" + nodes
                    + "\" changeTimeout=\"100\">" + two + bandits[bSetting] + "</streamingModule>");
          }
        }
      }
    }
    fileliststream.close();
  }
  
  private static String generatePushPull(final int chunkRequestSetting, final int overlaySetting, final String size) {
    return "<streamingModule algorithm=\"PushPull\" size=\"" + size + "\">" + generateTreeboneOverlay()
        + generatePull(chunkRequestSetting, overlaySetting, size) + "</streamingModule>";
  }
  
  private static String generatePull(final int chunkRequestSetting, final int overlaySetting, final String size) {
    return "<streamingModule algorithm=\"PullAlgorithm\" size=\"" + size + "\">" + overlays[overlaySetting]
        + "<ingredient name=\"SourcePushIngredient\"/>"
        + "<ingredient name=\"EarliestContinuousChunkVSInitIngredient\" maxOffset=\"0\"/>"
        + "<ingredient name=\"HandleChunkRequestsOnArrivalIngredient\"/>" + chunkRequest[chunkRequestSetting]
        + "<ingredient name=\"ChunkChoiceMeasurement\"/>" + "</streamingModule>";
  }
  
  static String generateBTlive(final String size) {
    return "<streamingModule algorithm=\"PushMultipleSubstreamTree\" sendMissingChunks=\"true\" size=\"" + size + "\">"
        + "<overlayModule clubsForPeer=\"2\""
        + " algorithm=\"BittorrentLiveOverlay\" minInClubDownload=\"2\" maxInClubDownload=\"3\" parentDropTimeout=\"0\" />"
        + "</streamingModule>";
  }
  
  static String generateTreeBone(final String size) {
    return "<streamingModule algorithm=\"PushTree\" size=\"" + size + "\">" + generateTreeboneOverlay() + "</streamingModule>";
  }
  
  static String generateTreeboneOverlay() {
    return "<overlayModule algorithm=\"TreeBone\" stableCoefficient=\"0.75\" queryTimeout=\"5\" freeSlots=\"1\" />";
  }
}
