package experiment;

import ingredients.AbstractIngredient;
import ingredients.DNVP.DNVPOverlayIngredient;
import ingredients.DNVP.Malicious1DNVPIngredient;
import ingredients.DNVP.Malicious2DNVPIngredinet;
import ingredients.omission.OmissionDefenceIngredient;
import ingredients.omission.OmissionDefenceIngredient2;
import ingredients.overlay.NeighborChunkAvailabilityIngredient;
import ingredients.overlay.NeighborChunkAvailabilityIngredient.OperationMode;
import ingredients.overlay.security.BlackListIngredient;
import ingredients.overlay.security.DNVPAuditingIngredient;
import ingredients.overlay.security.FreeridingAuditingIngredient;
import ingredients.streaming.ChunkRequestAlgoSwitchIngredient;
import ingredients.streaming.mandatory.ChainsawChunkRequestIngredient;
import ingredients.streaming.mandatory.CoolstreamingChunkRequestIngredient;
import ingredients.streaming.mandatory.EarliestContinuousChunkVSInitIngredient;
import ingredients.streaming.mandatory.HandleChunkRequestsOnArrivalIngredient;
import ingredients.streaming.mandatory.HandleChunkRequestsSentAscendingIngredient;
import ingredients.streaming.mandatory.PulseChunkRequestIngredient;
import ingredients.streaming.optional.AdaptiveAdaptivePlayoutIngredient;
import ingredients.streaming.optional.ChunkChoiceMeasurement;
import ingredients.streaming.optional.ConstantBufferSizeAdaptivePlayoutIngredient;
import ingredients.streaming.optional.ConstantLagAdaptivePlayoutIngredient;
import ingredients.streaming.optional.LookaheadSkipPolicyIngredient;
import ingredients.streaming.optional.SourcePushIngredient;
import ingredients.streaming.optional.WaitThenSkipPolicyIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.ArrayList;
import java.util.Random;

import modules.P2PClient;
import modules.overlays.Araneola;
import modules.overlays.BSCAMP;
import modules.overlays.BittorrentLiveOverlay;
import modules.overlays.ChunkySpread;
import modules.overlays.CoolStreamingOverlay;
import modules.overlays.CoolStreamingPlusOverlay;
import modules.overlays.ExperimentControl;
import modules.overlays.FastMeshOverlay;
import modules.overlays.GroupedOverlayModule;
import modules.overlays.MultipleTreeOverlayModule;
import modules.overlays.OverlayModule;
import modules.overlays.PrimeOverlay;
import modules.overlays.PulseOverlay;
import modules.overlays.RandomMinSizeGroupOverlay;
import modules.overlays.SCAMP;
import modules.overlays.SwapLinks;
import modules.overlays.TreeBone;
import modules.overlays.TreeOverlayModule;
import modules.streaming.AdaptiveStreaming;
import modules.streaming.EmptyStreamingModule;
import modules.streaming.PrimaryFallbackStreaming;
import modules.streaming.PrimeStreaming;
import modules.streaming.PushMultipleDescriptionTree;
import modules.streaming.PushMultipleSubstreamTree;
import modules.streaming.PushPull;
import modules.streaming.PushTree;
import modules.streaming.StreamingModule;
import modules.streaming.pullAlgorithms.Chainsaw;
import modules.streaming.pullAlgorithms.CoolStreaming;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import modules.streaming.pullAlgorithms.Pulse;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import utils.Common;
import utils.chunkySpread.LatencyMeasureFactory;
import bandits.algo.AlgoPerState;
import bandits.algo.BanditsAlgorithm;
import bandits.algo.ConstBanditsAlgo;
import bandits.algo.EpsilonGreedy;
import bandits.algo.EpsilonGreedy.ConstantEpsilon;
import bandits.algo.EpsilonGreedy.InvLinearEpsilon;
import bandits.algo.EpsilonGreedy.LinearEpsilon;
import bandits.algo.StatefullEpsilonGreedy;
import bandits.algo.UCB1;
import bandits.reward.ChunksReceivedAvailableReward;
import bandits.reward.ChunksReceivedReward;
import bandits.reward.InfoGather;
import bandits.reward.RewardIngredient;
import bandits.reward.SimpleCIReward;
import bandits.state.StateIngredient;
import bandits.state.UploadBandwidthSessionLengthState;
import experiment.ExperimentConfiguration.GetOverlayMod;
import experiment.ExperimentConfiguration.GetStreamingMod;

public class StreamingModuleFactory {
  public static GetStreamingMod initStreamingModule(final Node n) {
    if (!n.getNodeName().equals("streamingModule")) {
      throw new IllegalStateException("illegal node passed to initStreamingModule: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("algorithm")).getValue();
    if (alg.equals(CoolStreaming.class.getSimpleName())) {
      final boolean serverPush = XMLutils.getAttribute(n, "sourcePush", "true").equals("true");
      final int maxOffset = Integer.valueOf(XMLutils.getAttribute(n, "maxInitOffset", "0"));
      final int maxInitLatency = Integer.valueOf(XMLutils.getAttribute(n, "maxInitLatency", "10"));
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new CoolStreaming(client, overlayFunc.getAlg(client), serverPush, maxOffset,
              maxInitLatency, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + (serverPush ? "sp" : "") + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PrimaryFallbackStreaming.class.getSimpleName())) {
      int i;
      Node node1 = null;
      Node node2 = null;
      for (i = 0; i < n.getChildNodes().getLength(); i++) {
        if (n.getChildNodes().item(i).getNodeName().equals("streamingAlgorithm")) {
          if (node1 == null) {
            node1 = n.getChildNodes().item(i);
          } else {
            node2 = n.getChildNodes().item(i);
          }
        }
      }
      final GetStreamingMod mainAlg = initStreamingModule(node1);
      final GetStreamingMod pullAlg = initStreamingModule(node2);
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PrimaryFallbackStreaming(client, mainAlg.getAlg(client),
              (PullAlgorithm) pullAlg.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + mainAlg.getName() + "-" + pullAlg.getName();
        }
      };
    } else if (alg.equals(PushPull.class.getSimpleName())) {
      int i;
      Node node1 = null;
      Node node2 = null;
      for (i = 0; i < n.getChildNodes().getLength(); i++) {
        if (n.getChildNodes().item(i).getNodeName().equals("overlayModule")) {
          node1 = n.getChildNodes().item(i);
        }
        if (n.getChildNodes().item(i).getNodeName().equals("streamingModule")) {
          node2 = n.getChildNodes().item(i);
        }
      }
      final GetOverlayMod overlayFunc = initOverlayFunction(node1);
      final GetStreamingMod mainAlg = initStreamingModule(node2);
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PushPull(client, (TreeOverlayModule<?>) overlayFunc.getAlg(client),
              (PullAlgorithm) mainAlg.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + mainAlg.getName() + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PushTree.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PushTree(client, (TreeOverlayModule<?>) overlayFunc.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PrimeStreaming.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PrimeStreaming(client, overlayFunc.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(EmptyStreamingModule.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new EmptyStreamingModule(client, overlayFunc.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(Chainsaw.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      final boolean serverPush = XMLutils.getAttribute(n, "serverPush", "true").equals("true");
      final int maxOffset = Integer.valueOf(XMLutils.getAttribute(n, "maxInitOffset", "0"));
      final int maxInitLatency = Integer.valueOf(XMLutils.getAttribute(n, "maxInitLatency", "10"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new Chainsaw(client, overlayFunc.getAlg(client), serverPush, maxOffset, maxInitLatency, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(Pulse.class.getSimpleName())) {
      final int reqLimit = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("reqLimit")).getValue());
      final boolean serverPush = XMLutils.getAttribute(n, "serverPush", "true").equals("true");
      final int maxOffset = Integer.valueOf(XMLutils.getAttribute(n, "maxInitOffset", "0"));
      final int maxInitLatency = Integer.valueOf(XMLutils.getAttribute(n, "maxInitLatency", "10"));
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new Pulse(client, (GroupedOverlayModule<?>) overlayFunc.getAlg(client), reqLimit,
              serverPush, maxOffset, maxInitLatency, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PushMultipleDescriptionTree.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PushMultipleDescriptionTree(client,
              (MultipleTreeOverlayModule<?>) overlayFunc.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PushMultipleSubstreamTree.class.getSimpleName())) {
      final boolean sendMissingChunks = XMLutils.getAttribute(n, "sendMissingChunks", "false").equals("true");
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PushMultipleSubstreamTree(client,
              (MultipleTreeOverlayModule<?>) overlayFunc.getAlg(client), sendMissingChunks, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(PullAlgorithm.class.getSimpleName())) {
      final GetOverlayMod overlayFunc = initOverlayFunction(XMLutils.findFirstChild(n, "overlayModule"));
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final StreamingModule retVal = new PullAlgorithm(client, overlayFunc.getAlg(client), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + overlayFunc.getName();
        }
      };
    } else if (alg.equals(AdaptiveStreaming.class.getSimpleName())) {
      final ArrayList<Node> options = new ArrayList<Node>();
      final int changeTimeout = Integer.parseInt(XMLutils.getAttribute(n, "changeTimeout", "50"));
      int baIndex = -1;
      for (int i = 0; i < n.getChildNodes().getLength(); i++) {
        if (n.getChildNodes().item(i).getNodeName().equals("banditsAlgo")) {
          baIndex = i;
        } else if (n.getChildNodes().item(i).getNodeName().equals("streamingModule")) {
          options.add(n.getChildNodes().item(i));
        }
      }
      final Node banditsAlgoNode = n.getChildNodes().item(baIndex);
      return new GetStreamingMod() {
        @Override public StreamingModule getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final BanditsAlgorithm banditsAlgo = initBanditsAlgo(banditsAlgoNode, options.size(), new Random(r.nextLong()));
          final StreamingModule retVal = new AdaptiveStreaming(client, options, changeTimeout, banditsAlgo, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "-" + options.size();
        }
      };
    } else {
      throw new IllegalStateException("unrecognized streamingAlgorithm - " + alg);
    }
  }
  
  public static GetOverlayMod initOverlayFunction(final Node n) {
    if (!n.getNodeName().equals("overlayModule")) {
      throw new IllegalStateException("illegal node passed to initOverlayFunction: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("algorithm")).getValue();
    if (alg.equals(SCAMP.class.getSimpleName())) {
      final String c = ((Attr) n.getAttributes().getNamedItem("c")).getValue();
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new SCAMP(client, Integer.parseInt(c), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + c;
        }
      };
    } else if (alg.equals(BSCAMP.class.getSimpleName())) {
      final String c = ((Attr) n.getAttributes().getNamedItem("c")).getValue();
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new BSCAMP(client, Integer.parseInt(c), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + c;
        }
      };
    } else if (alg.equals(Araneola.class.getSimpleName())) {
      final int S = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("S")).getValue());
      final int gossipDelay = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("gossipDelay")).getValue());
      final int amountToSend = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("amountToSend")).getValue());
      final int L = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("L")).getValue());
      // final int H = Integer.parseInt(((Attr)
      // n.getAttributes().getNamedItem("H")).getValue());
      final int connect_timeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("connect_timeout")).getValue());
      final int disconnect_timeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("disconnect_timeout")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new Araneola(client, S, gossipDelay, amountToSend, L, Math.max(
              Math.max((int) client.network.getUploadBandwidth() / Common.currentConfiguration.bitRate, 1), L + 1),
              connect_timeout, disconnect_timeout, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "L" + (L < 10 ? "0" : "") + L;
        }
      };
    } else if (alg.equals(TreeBone.class.getSimpleName())) {
      final double stableCoefficient = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("stableCoefficient")).getValue());
      final int queryTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("queryTimeout")).getValue());
      final int freeSlots = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("freeSlots")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new TreeBone(client, stableCoefficient, queryTimeout, freeSlots, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "coef" + stableCoefficient + "fs" + freeSlots;
        }
      };
    } else if (alg.equals(PrimeOverlay.class.getSimpleName())) {
      final String size = ((Attr) n.getAttributes().getNamedItem("size")).getValue();
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new PrimeOverlay(client, Integer.parseInt(size), r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg;
        }
      };
    } else if (alg.equals(CoolStreamingOverlay.class.getSimpleName())) {
      final int M = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("M")).getValue());
      final int H = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("H")).getValue());
      final int c = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("c")).getValue());
      final int exploreRound = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("exploreRound")).getValue());
      final int gossipTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("gossipTimeout")).getValue());
      final int amountToSend = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("amountToSend")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new CoolStreamingOverlay(client, c, M, H, amountToSend, exploreRound, gossipTimeout, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "M" + M;
        }
      };
    } else if (alg.equals(ChunkySpread.class.getSimpleName())) {
      Node swapLinksElement = null;
      for (int i = 0; i < n.getChildNodes().getLength(); i++) {
        if (n.getChildNodes().item(i).getNodeName().equals("overlayModule")) {
          swapLinksElement = n.getChildNodes().item(i);
        }
      }
      final GetOverlayMod swapLinksGetter = initOverlayFunction(swapLinksElement);
      final int maxLoad = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("maxLoad")).getValue());
      // upper latency threshold
      final double ULT = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("ULT")).getValue());
      // lower latency threshold
      final double LLT = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("LLT")).getValue());
      if (ULT < LLT || ULT > 1 || LLT < 0) {
        throw new AssertionError();
      }
      final int startDelay = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("startDelay")).getValue());
      final int churnDelay = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("churnDelay")).getValue());
      final int noParentTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("noParentTimeout")).getValue());
      final int pendingTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("pendingTimeout")).getValue());
      final int windowSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("windowSize")).getValue());
      final double fastPercentage = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("fastPercentage")).getValue());
      final double slowPercentage = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("slowPercentage")).getValue());
      final int highScore = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("highScore")).getValue());
      final int lowScore = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("lowScore")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final LatencyMeasureFactory latencyMeasureFactory = new LatencyMeasureFactory(windowSize,
              Common.currentConfiguration.descriptions, fastPercentage, slowPercentage, highScore, lowScore);
          final OverlayModule<?> chunkySpread = new ChunkySpread(client, swapLinksGetter.getAlg(client), maxLoad, ULT, LLT,
              startDelay, churnDelay, noParentTimeout, pendingTimeout, latencyMeasureFactory, r);
          getIngredients(chunkySpread, n, r, client);
          return chunkySpread;
        }
        
        @Override public String getName() {
          return alg;
        }
      };
    } else if (alg.equals(CoolStreamingPlusOverlay.class.getSimpleName())) {
      final int M = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("M")).getValue());
      // final int c = Integer.parseInt(((Attr)
      // n.getAttributes().getNamedItem("c")).getValue());
      final int exploreRound = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("exploreRound")).getValue());
      final int bitMapTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("bitMapTimeout")).getValue());
      final int ts = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("ts")).getValue());
      final int ta = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("ta")).getValue());
      final int tp = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("tp")).getValue());
      final int bitMapCyclesForPartnerToBeSufficient = Integer.parseInt(((Attr) n.getAttributes().getNamedItem(
          "bitMapCyclesForPartnerToBeSufficient")).getValue());
      final double contactRatio = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("contactRatio")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new CoolStreamingPlusOverlay(client, M, Common.currentConfiguration.descriptions,
              exploreRound, bitMapTimeout, contactRatio, ta, ts, tp, bitMapCyclesForPartnerToBeSufficient, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "M" + M;
        }
      };
    } else if (alg.equals(SwapLinks.class.getSimpleName())) {
      final int wl = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("wl")).getValue());
      final int groupSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("groupSize")).getValue());
      final long minCapacity = Long.parseLong(((Attr) n.getAttributes().getNamedItem("minCapacity")).getValue());
      final long minUploadBandwidth = Long.parseLong(((Attr) n.getAttributes().getNamedItem("minUploadBandwidth")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new SwapLinks(client, wl, groupSize, minCapacity, minUploadBandwidth, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "wl" + wl;
        }
      };
    } else if (alg.equals(BittorrentLiveOverlay.class.getSimpleName())) {
      final int clubsForPeer = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("clubsForPeer")).getValue());
      final int minInClubDownload = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("minInClubDownload")).getValue());
      final int maxInClubDownload = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("maxInClubDownload")).getValue());
      final int parentDropTimeout = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("parentDropTimeout")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new BittorrentLiveOverlay(client, clubsForPeer, minInClubDownload, maxInClubDownload,
              parentDropTimeout, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "d" + minInClubDownload + "-" + maxInClubDownload + "cp" + clubsForPeer;
        }
      };
    } else if (alg.equals(RandomMinSizeGroupOverlay.class.getSimpleName())) {// TODO
      // document
      final int groupSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("groupSize")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new RandomMinSizeGroupOverlay(client, groupSize, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "S" + groupSize;
        }
      };
    } else if (alg.equals(PulseOverlay.class.getSimpleName())) { // TODO
      // document
      final int c = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("c")).getValue());
      final int missing = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("missing")).getValue());
      final int forward = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("forward")).getValue());
      final int epoch = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("epoch")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new PulseOverlay(client, c, missing, forward, epoch, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "c" + c + "m" + missing + "f" + forward + "e" + epoch;
        }
      };
    } else if (alg.equals(FastMeshOverlay.class.getSimpleName())) { // TODO
      // document
      final int groupSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("groupSize")).getValue());
      final int TTL = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("TTL")).getValue());
      final int TEBA = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("TEBA")).getValue());
      final int TTWFG = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("TTWFG")).getValue());
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          final OverlayModule<?> retVal = new FastMeshOverlay(client, groupSize, TTL, TEBA, TTWFG, r);
          getIngredients(retVal, n, r, client);
          return retVal;
        }
        
        @Override public String getName() {
          return alg + "g" + groupSize;
        }
      };
    } else if (alg.equals(ExperimentControl.class.getSimpleName())) {// TODO
      // document
      return new GetOverlayMod() {
        @Override public OverlayModule<?> getAlg(final P2PClient client) {
          final Random r = new Random(Common.currentConfiguration.experimentRandom.nextLong());
          return new ExperimentControl(client, r);
        }
        
        @Override public String getName() {
          return alg;
        }
      };
    } else {
      throw new IllegalStateException("unknown overlay " + alg);
    }
  }
  
  public static void getIngredients(final NodeConnectionAlgorithm ncl, final Node n, final Random r, final P2PClient client) {
    for (int i = 0; i < n.getChildNodes().getLength(); i++) {
      final Node n2 = n.getChildNodes().item(i);
      if (!n2.getNodeName().equals("ingredient")) {
        continue;
      }
      initIngredient(ncl, n2, new Random(r.nextLong()), client);
    }
  }
  
  public static AbstractIngredient<? extends NodeConnectionAlgorithm> initIngredient(final NodeConnectionAlgorithm ncl,
      final Node n, final Random r, final P2PClient client) {
    AbstractIngredient<? extends NodeConnectionAlgorithm> retVal;
    final String name = ((Attr) n.getAttributes().getNamedItem("name")).getValue();
    if (name.equals(DNVPOverlayIngredient.class.getSimpleName())) {
      final int timeoutcycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("timeoutcycle")).getValue());
      final int expirationInterval = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("expirationInterval")).getValue());
      final int numOfNoncesToProduce = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numOfNoncesToProduce")).getValue());
      final double checkApproval = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("checkApproval")).getValue());
      final double checkDisconnection = Double
          .parseDouble(((Attr) n.getAttributes().getNamedItem("checkDisconnection")).getValue());
      final int verificationsPerCycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("verificationsPerCycle"))
          .getValue());
      ncl.addIngredient(retVal = new DNVPOverlayIngredient(timeoutcycle, expirationInterval, numOfNoncesToProduce, checkApproval,
          checkDisconnection, verificationsPerCycle, new Random(r.nextLong())), client);
    } else if (name.equals(Malicious1DNVPIngredient.class.getSimpleName())) {
      final int timeoutcycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("timeoutcycle")).getValue());
      final int expirationInterval = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("expirationInterval")).getValue());
      final int numOfNoncesToProduce = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numOfNoncesToProduce")).getValue());
      final double checkApproval = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("checkApproval")).getValue());
      final double checkDisconnection = Double
          .parseDouble(((Attr) n.getAttributes().getNamedItem("checkDisconnection")).getValue());
      final int verificationsPerCycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("verificationsPerCycle"))
          .getValue());
      ncl.addIngredient(retVal = new Malicious1DNVPIngredient(timeoutcycle, expirationInterval, numOfNoncesToProduce,
          checkApproval, checkDisconnection, verificationsPerCycle, new Random(r.nextLong())), client);
    } else if (name.equals(Malicious2DNVPIngredinet.class.getSimpleName())) {
      final int timeoutcycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("timeoutcycle")).getValue());
      final int expirationInterval = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("expirationInterval")).getValue());
      final int numOfNoncesToProduce = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numOfNoncesToProduce")).getValue());
      final double checkApproval = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("checkApproval")).getValue());
      final double checkDisconnection = Double
          .parseDouble(((Attr) n.getAttributes().getNamedItem("checkDisconnection")).getValue());
      final int verificationsPerCycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("verificationsPerCycle"))
          .getValue());
      ncl.addIngredient(retVal = new Malicious2DNVPIngredinet(timeoutcycle, expirationInterval, numOfNoncesToProduce,
          checkApproval, checkDisconnection, verificationsPerCycle, new Random(r.nextLong())), client);
    } else if (name.equals(OmissionDefenceIngredient.class.getSimpleName())) {
      final int numOfCycles = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numOfCycles")).getValue());
      final int thresholdForMisses = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("thresholdForMisses")).getValue());
      final int roundsForDiffusion = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("roundsForDiffusion")).getValue());
      final int roundsForAnswer = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("roundsForAnswer")).getValue());
      ncl.addIngredient(retVal = new OmissionDefenceIngredient(numOfCycles, thresholdForMisses, roundsForDiffusion,
          roundsForAnswer, new Random(r.nextLong())), client);
    } else if (name.equals(OmissionDefenceIngredient2.class.getSimpleName())) {
      final int numOfCycles = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numOfCycles")).getValue());
      final int thresholdForMisses = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("thresholdForMisses")).getValue());
      final int roundsForBitmap = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("roundsForBitmap")).getValue());
      final int roundsForAnswer = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("roundsForAnswer")).getValue());
      final int verificationsPerCycle = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("verificationsPerCycle"))
          .getValue());
      ncl.addIngredient(retVal = new OmissionDefenceIngredient2(numOfCycles, thresholdForMisses, roundsForBitmap, roundsForAnswer,
          verificationsPerCycle, new Random(r.nextLong())), client);
    } else if (name.equals(DNVPAuditingIngredient.class.getSimpleName())) {
      final int thresholdForSource = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("thresholdForSource")).getValue());
      final double thresholdForCommittee = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("thresholdForCommittee"))
          .getValue());
      ncl.addIngredient(retVal = new DNVPAuditingIngredient(thresholdForSource, thresholdForCommittee, new Random(r.nextLong())),
          client);
    } else if (name.equals(FreeridingAuditingIngredient.class.getSimpleName())) {
      final int thresholdForSource = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("thresholdForSource")).getValue());
      ncl.addIngredient(retVal = new FreeridingAuditingIngredient(thresholdForSource, new Random(r.nextLong())), client);
    } else if (name.equals(BlackListIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new BlackListIngredient(), client);
    } else if (name.equals(ConstantBufferSizeAdaptivePlayoutIngredient.class.getSimpleName())) {
      final int bufferSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("bufferSize")).getValue());
      final double maxSpeedChange = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("maxSpeedChange")).getValue());
      ncl.addIngredient(
          retVal = new ConstantBufferSizeAdaptivePlayoutIngredient(bufferSize, maxSpeedChange, new Random(r.nextLong())), client);
    } else if (name.equals(ConstantLagAdaptivePlayoutIngredient.class.getSimpleName())) {
      final int minTargetLag = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("minTargetLag")).getValue());
      final int maxTargetLag = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("maxTargetLag")).getValue());
      final double maxSpeedChange = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("maxSpeedChange")).getValue());
      ncl.addIngredient(
          retVal = new ConstantLagAdaptivePlayoutIngredient(minTargetLag, maxTargetLag, maxSpeedChange, new Random(r.nextLong())),
          client);
    } else if (name.equals(LookaheadSkipPolicyIngredient.class.getSimpleName())) {
      final int numberOfLookaheadChunks = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("numberOfLookaheadChunks"))
          .getValue());
      ncl.addIngredient(retVal = new LookaheadSkipPolicyIngredient(numberOfLookaheadChunks, new Random(r.nextLong())), client);
    } else if (name.equals(WaitThenSkipPolicyIngredient.class.getSimpleName())) {
      final int waitPeriod = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("waitPeriod")).getValue());
      ncl.addIngredient(retVal = new WaitThenSkipPolicyIngredient(waitPeriod, new Random(r.nextLong())), client);
    } else if (name.equals(NeighborChunkAvailabilityIngredient.class.getSimpleName())) {
      final String operationMode = ((Attr) n.getAttributes().getNamedItem("operationMode")).getValue();
      final boolean ssb = n.getAttributes().getNamedItem("serverSendsBitmaps") != null;
      ncl.addIngredient(
          retVal = new NeighborChunkAvailabilityIngredient(OperationMode.valueOf(operationMode), ssb, new Random(r.nextLong())),
          client);
    } else if (name.equals(SourcePushIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new SourcePushIngredient(new Random(r.nextLong())), client);
    } else if (name.equals(EarliestContinuousChunkVSInitIngredient.class.getSimpleName())) {
      final int maxOffset = Integer.parseInt(XMLutils.getAttribute(n, "maxOffset", "0"));
      final int maxLatency = Integer.parseInt(XMLutils.getAttribute(n, "maxLatency", "10"));
      ncl.addIngredient(retVal = new EarliestContinuousChunkVSInitIngredient(maxOffset, maxLatency, new Random(r.nextLong())),
          client);
    } else if (name.equals(HandleChunkRequestsOnArrivalIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new HandleChunkRequestsOnArrivalIngredient(new Random(r.nextLong())), client);
    } else if (name.equals(CoolstreamingChunkRequestIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new CoolstreamingChunkRequestIngredient(new Random(r.nextLong())), client);
    } else if (name.equals(ChainsawChunkRequestIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new ChainsawChunkRequestIngredient(new Random(r.nextLong())), client);
    } else if (name.equals(PulseChunkRequestIngredient.class.getSimpleName())) {
      final int requestFromNodeLimit = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("requestFromNodeLimit")).getValue());
      ncl.addIngredient(retVal = new PulseChunkRequestIngredient(requestFromNodeLimit, new Random(r.nextLong())), client);
    } else if (name.equals(AdaptiveAdaptivePlayoutIngredient.class.getSimpleName())) {
      final int bufferSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("bufferSize")).getValue());
      final double maxSpeedChange = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("maxSpeedChange")).getValue());
      final int initRounds = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("initRounds")).getValue());
      final double alpha = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("alpha")).getValue());
      ncl.addIngredient(
          retVal = new AdaptiveAdaptivePlayoutIngredient(bufferSize, maxSpeedChange, initRounds, alpha, new Random(r.nextLong())),
          client);
    } else if (name.equals(HandleChunkRequestsSentAscendingIngredient.class.getSimpleName())) {
      ncl.addIngredient(retVal = new HandleChunkRequestsSentAscendingIngredient(new Random(r.nextLong())), client);
    } else if (name.equals(ChunkRequestAlgoSwitchIngredient.class.getSimpleName())) {
      BanditsAlgorithm banditsAlgo = null;
      final ArrayList<Node> options = new ArrayList<Node>();
      final int timeout = Integer.parseInt(XMLutils.getAttribute(n, "timeout", "50"));
      for (int i = 0; i < n.getChildNodes().getLength(); i++) {
        if (n.getChildNodes().item(i).getNodeName().equals("banditsAlgo")) {
          banditsAlgo = initBanditsAlgo(n.getChildNodes().item(i), options.size(), new Random(r.nextLong()));
        } else if (n.getChildNodes().item(i).getNodeName().equals("ingredient")) {
          options.add(n.getChildNodes().item(i));
        }
      }
      ncl.addIngredient(retVal = new ChunkRequestAlgoSwitchIngredient(options, banditsAlgo, timeout, new Random(r.nextLong())),
          client);
    } else if (name.equals(ChunkChoiceMeasurement.class.getSimpleName())) {
      ncl.addIngredient(retVal = new ChunkChoiceMeasurement(), client);
    } else {
      retVal = null;
    }
    if (retVal == null) {
      throw new IllegalStateException("illegal ingredient passed to initIngredient: " + name);
    }
    return retVal;
  }
  
  static BanditsAlgorithm initBanditsAlgo(final Node n, final int optionsNum, final Random r) {
    if (!n.getNodeName().equals("banditsAlgo")) {
      throw new IllegalStateException("illegal node passed to initBanditsAlgo: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("name")).getValue();
    if (alg.equals(UCB1.class.getSimpleName())) {
      final int conv = Integer.parseInt(XMLutils.getAttribute(n, "conv", "0"));
      return new UCB1(initBanditReward(XMLutils.findFirstChild(n, "banditReward")), optionsNum, conv);
    } else if (alg.equals(ConstBanditsAlgo.class.getSimpleName())) {
      final int opt = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("option")).getValue());
      return new ConstBanditsAlgo(opt);
    } else if (alg.equals(EpsilonGreedy.class.getSimpleName())) {
      String att;
      final RewardIngredient rew = initBanditReward(XMLutils.findFirstChild(n, "banditReward"));
      if ((att = XMLutils.getAttribute(n, "constant", null)) != null) {
        return new EpsilonGreedy(new Random(r.nextLong()), rew, optionsNum, new ConstantEpsilon(Double.parseDouble(att)));
      }
      if ((att = XMLutils.getAttribute(n, "linear", null)) != null) {
        return new EpsilonGreedy(new Random(r.nextLong()), rew, optionsNum, new LinearEpsilon(Double.parseDouble(att)));
      }
      if ((att = XMLutils.getAttribute(n, "invlinear", null)) != null) {
        return new EpsilonGreedy(new Random(r.nextLong()), rew, optionsNum, new InvLinearEpsilon(Double.parseDouble(att)));
      }
      throw new RuntimeException("EpsilonGreedy must have constant, linear or invlinear attribute");
    } else if (alg.equals(StatefullEpsilonGreedy.class.getSimpleName())) {
      return new StatefullEpsilonGreedy(new Random(r.nextLong()), initBanditState(XMLutils.findFirstChild(n, "banditState")),
          initBanditReward(XMLutils.findFirstChild(n, "banditReward")), optionsNum);
    } else if (alg.equals(AlgoPerState.class.getSimpleName())) {
      final int def = Integer.parseInt(XMLutils.getAttribute(n, "default", "0"));
      return new AlgoPerState(initBanditState(XMLutils.findFirstChild(n, "banditState")), def, optionsNum);
    }
    throw new IllegalStateException("illegal algorithm passed to initBanditsAlgo: " + alg);
  }
  
  private static StateIngredient initBanditState(final Node n) {
    if (!n.getNodeName().equals("banditState")) {
      throw new IllegalStateException("illegal node passed to banditState: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("name")).getValue();
    if (alg.equals(UploadBandwidthSessionLengthState.class.getSimpleName())) {
      return new UploadBandwidthSessionLengthState();
    }
    throw new IllegalStateException("illegal algorithm passed to initBanditState: " + alg);
  }
  
  private static RewardIngredient initBanditReward(final Node n) {
    if (!n.getNodeName().equals("banditReward")) {
      throw new IllegalStateException("illegal node passed to initBanditReward: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("name")).getValue();
    if (alg.equals(ChunksReceivedReward.class.getSimpleName())) {
      return new ChunksReceivedReward();
    } else if (alg.equals(ChunksReceivedAvailableReward.class.getSimpleName())) {
      return new ChunksReceivedAvailableReward();
    } else if (alg.equals(SimpleCIReward.class.getSimpleName())) {
      return new SimpleCIReward();
    } else if (alg.equals(InfoGather.class.getSimpleName())) {
      final int timeout = Integer.parseInt(XMLutils.getAttribute(n, "timeout", "10"));
      final ArrayList<RewardIngredient> rewards = new ArrayList<RewardIngredient>();
      for (final Node node : XMLutils.findAllChildren(n, "banditReward")) {
        rewards.add(initBanditReward(node));
      }
      return new InfoGather(rewards, timeout);
    }
    throw new IllegalStateException("illegal algorithm passed to initBanditReward: " + alg);
  }
}
