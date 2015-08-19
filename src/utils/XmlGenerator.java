package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import experiment.ExperimentConfiguration;

public class XmlGenerator {
  static final int playbackSeconds = 600;
  static final int runs = 10;
  static final String dir = "stateTest7";
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
    "<serverUploadBandwidth value=\"20000000\"/>\n"
        + "<uploadBandwidthDistribution> <distribution type=\"NormalDistribution\" mean=\"12600000\" variance=\"250000000000\" />  </uploadBandwidthDistribution>",
        /* "<serverUploadBandwidth value=\"20000000\"/>\n" +
         * "<uploadBandwidthDistribution> <distribution type=\"NormalDistribution\" mean=\"6600000\" variance=\"4000000000000\" />  </uploadBandwidthDistribution>"
         * , */
        "<serverUploadBandwidth value=\"20000000\"/>\n"
        + "<uploadBandwidthDistribution> <distribution type=\"NormalDistribution\" mean=\"2600000\" variance=\"250000000000\" />  </uploadBandwidthDistribution>" };
  static final String[] churn = {
    "<churnModel type=\"sessionLengthAddOnFailure\"><sessionLengthDistribution><distribution type=\"LogNormalDistribution\" "
        + "mean=\"6.29\" variance=\"1.28\" /> </sessionLengthDistribution>  </churnModel>\n",
        "<churnModel type=\"sessionLengthAddOnFailure\"><sessionLengthDistribution><distribution type=\"LogNormalDistribution\" "
            + "mean=\"3.29\" variance=\"1.28\" /> </sessionLengthDistribution>  </churnModel>\n" };
  static final String[] player = {
    "<playerModule waitIfNoChunk=\"\" bufferFromFirstChunk=\"true\" serverStartup=\"10000\" startupBuffering=\"2000\" streamWindowSize=\"120\" />",
    "<playerModule bufferFromFirstChunk=\"true\" serverStartup=\"10000\" startupBuffering=\"2000\" streamWindowSize=\"120\" />",
    "<playerModule waitIfNoChunk=\"\" bufferFromFirstChunk=\"true\" serverStartup=\"10000\" startupBuffering=\"1000\" streamWindowSize=\"120\" />",
  "<playerModule bufferFromFirstChunk=\"true\" serverStartup=\"10000\" startupBuffering=\"1000\" streamWindowSize=\"120\" />" };
  static final String[] playerName = { "wait2", "skip2", "wait1", "skip1" };
  static final String[] networkSize = { "200" };
  static final String[] chunkRequest = { "<ingredient name=\"ChainsawChunkRequestIngredient\"/>",
  "<ingredient name=\"CoolstreamingChunkRequestIngredient\"/>" };
  static final String[] chunkRequestName = { "ChainSaw", "Coolstreaming" };
  
  static void generateSingleXml(final PrintWriter fileliststream, final String name, final int uploadSetting,
      final int churnSetting, final int playerSetting, final String streamingModule) throws FileNotFoundException {
    final StringBuffer sb = new StringBuffer();
    sb.append("<experiment>\n" + "<name value=\"" + name + "\" />\n" + "<seed value=\"12345678\" />\n"
        + "<playbackSeconds value=\"" + playbackSeconds + "\" />" + "<runs value=\"" + runs + "\" />\n"
        + "<messageHeaderSize value=\"64\" />\n" + "<cycleLength value=\"1000\" />\n"
        + "<framework value=\"PeerSimFramework\" dropRate=\"0.0\" />\n" + "<bitrate value=\"500000\" />\n"
        + "<descriptions value=\"6\" />\n<enableWaitQueueLimit/>\n");
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
      final ExperimentConfiguration conf1 = new ExperimentConfiguration(sb.toString());
      final ExperimentConfiguration conf2 = new ExperimentConfiguration(conf1.toXml());
      if (!conf1.equals(conf2)) {
        throw new IllegalStateException("confs are different!");
      }
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  // ch0u0wait-ChainSawRandomMinSizepull
  // ch0u1wait-CoolstreamingCoolstreaming7pushpull
  // ch1u0wait-CoolstreamingCoolstreaming7pull
  // ch1u1wait-CoolstreamingRandomMinSizepushpull
  public static void main(final String[] args) throws FileNotFoundException {
    if (!new File(dir).exists()) {
      new File(dir).mkdirs();
    }
    final int[] bestChunkRequestSetting = new int[] { 0, 1, 1, 1 };
    final int[] bestOverlaySetting = new int[] { 2, 1, 1, 2 };
    final boolean[] bestpushpull = new boolean[] { false, true, false, true };
    String two = "";
    for (int i = 0; i < 2; ++i) {
      two += bestpushpull[i] ? generatePushPull(bestChunkRequestSetting[i], bestOverlaySetting[i], "1000") : generatePull(
          bestChunkRequestSetting[i], bestOverlaySetting[i], "1000");
    }
    String all = "";
    for (int i = 0; i < bestpushpull.length; ++i) {
      all += bestpushpull[i] ? generatePushPull(bestChunkRequestSetting[i], bestOverlaySetting[i], "1000") : generatePull(
          bestChunkRequestSetting[i], bestOverlaySetting[i], "1000");
    }
    final int i = 0;
    final PrintWriter fileliststream = new PrintWriter(dir + File.separator + "fileList.list");
    for (int churnSetting = 0; churnSetting < churn.length; ++churnSetting) {
      for (final String nodes : networkSize) {
        for (int uploadSetting = 0; uploadSetting < uploadBandwidth.length; ++uploadSetting) {
          final String name = "ch" + churnSetting + "u" + uploadSetting;
          for (int playerSetting = 0; playerSetting < player.length; ++playerSetting) {
            for (int chunkRequestSetting = 0; chunkRequestSetting < chunkRequest.length; ++chunkRequestSetting) {
              for (int overlaySetting = 0; overlaySetting < overlays.length; ++overlaySetting) {
                generateSingleXml(fileliststream, name + "-" + playerName[playerSetting] + "-"
                    + chunkRequestName[chunkRequestSetting] + "-" + overlayName[overlaySetting] + "-" + "pull", uploadSetting,
                    churnSetting, playerSetting, generatePull(chunkRequestSetting, overlaySetting, nodes));
                generateSingleXml(fileliststream, name + "-" + playerName[playerSetting] + "-"
                    + chunkRequestName[chunkRequestSetting] + "-" + overlayName[overlaySetting] + "-" + "pushpull", uploadSetting,
                    churnSetting, playerSetting, generatePushPull(chunkRequestSetting, overlaySetting, nodes));
              }
            }
          }
          /* final int playerSetting = 0; generateSingleXml( fileliststream,
           * name + "-" + playerName[playerSetting] + "-" +
           * chunkRequestName[bestChunkRequestSetting[i]] + "-" +
           * overlayName[bestOverlaySetting[i]] + "-" + (bestpushpull[i] ?
           * "pushpull" : "pull"), uploadSetting, churnSetting, playerSetting,
           * bestpushpull[i] ? generatePushPull(bestChunkRequestSetting[i],
           * bestOverlaySetting[i], nodes) : generatePull(
           * bestChunkRequestSetting[i], bestOverlaySetting[i], nodes)); i++;
           * for (final String ct : changeTimeout) { for (int bSetting = 0;
           * bSetting < bandits.length; bSetting++) {
           * generateSingleXml(fileliststream, name + "-" +
           * playerName[playerSetting] + "-" + "MAB2_" + ct + "_" +
           * banditsName[bSetting], uploadSetting, churnSetting, playerSetting,
           * "<streamingModule algorithm=\"AdaptiveStreaming\" size=\"" + nodes
           * + "\" changeTimeout=\"" + ct + "\">" + two + bandits[bSetting] +
           * "</streamingModule>"); } for (int bSetting = 0; bSetting <
           * bandits.length; bSetting++) { generateSingleXml(fileliststream,
           * name + "-" + playerName[playerSetting] + "-" + "MAB4_" + ct + "_" +
           * banditsName[bSetting], uploadSetting, churnSetting, playerSetting,
           * "<streamingModule algorithm=\"AdaptiveStreaming\" size=\"" + nodes
           * + "\" changeTimeout=\"" + ct + "\">" + all + bandits[bSetting] +
           * "</streamingModule>"); } } */
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
