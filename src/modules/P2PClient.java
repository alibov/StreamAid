package modules;

import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import logging.TextLogger;
import modules.network.NetworkModule;
import modules.player.PlayerModule;
import modules.streaming.StreamingModule;
import utils.Common;

public class P2PClient extends NodeConnectionAlgorithm {
  public final StreamingModule streamingModule;
  public final PlayerModule player;
  public int cycleNumber = 0;
  
  public P2PClient(final NetworkModule node, final Random r) {
    super(node, r);
    player = Common.currentConfiguration.getPlayerModule(this);
    streamingModule = Common.currentConfiguration.getStreamingModule(this);
    // addIngredient(new InfoGatherIngredient(), this);
    // final boolean sourcePush = false;
    // no chunks requests sent
    /* options.add(new GetStreamingMod() {
     * 
     * @Override public String getName() { return null; }
     * 
     * @Override public StreamingModule getAlg(final P2PClient client) { //
     * OverlayModule<?> mainOverlay = ((PushPull) //
     * (streamingModules.getFirst())).pullAlg.mainOverlay; final
     * OverlayModule<?> mainOverlay =
     * client.streamingModules.getFirst().mainOverlay; final PullAlgorithm
     * retVal = new CoolStreaming(client, mainOverlay, sourcePush, maxOffset,
     * maxInitLatency) {
     * 
     * @Override public String getMessageTag() { return "PullAlgorithm"; } };
     * retVal
     * .getIngredient(CoolstreamingChunkReuqestIngredient.class).deactivate();
     * return retVal; } }); */
    // final BanditsAlgorithm banditsAlgo;
    // banditsAlgo = new EpsilonGreedy(new Random(r.nextLong()), null, new
    // SimpleCIReward(new Random(r.nextLong())), options.size());
    /* banditsAlgo = new StateRewardTest(new
     * ChunkReceivedDifferenceState(streamingModules.getFirst().mainOverlay),
     * new ChunksReceivedReward(), options.size()); */
    // banditsAlgo = new UCB1(new Random(r.nextLong()), new
    // ChunksReceivedReward(new Random(r.nextLong())), options.size());
    // banditsAlgo = new Exp3(new
    // Random(Common.currentConfiguration.experimentRandom.nextLong()), null,
    // new ChunksReceivedReward(),
    // options.size());
    // streamingModules.getFirst().addIngredient(new
    // StreamingAlgoSwitchIngredient(options, banditsAlgo), this);
    // streamingModules.getFirst().addIngredient(banditsAlgo.reward, this);
    // addIngredient(new ClientStreamingAlgoSwitchIngredient(options,
    // banditsAlgo, new Random(r.nextLong())), this);
    // addIngredient(banditsAlgo.reward, this);
    // if (banditsAlgo.state != null) {
    // streamingModules.getFirst().addIngredient(banditsAlgo.state, this);
    // addIngredient(banditsAlgo.state, this);
    // }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    TextLogger.log(network.getAddress(), "P2P client starting cycle " + cycleNumber + "\n");
    player.beforeStreamingCycle();
    streamingModule.nextCycle();
    player.afterStreamingCycle();
    cycleNumber++;
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    player.setServerMode();
    streamingModule.setServerMode();
  }
  
  public boolean isServerMode() {
    return network.isServerMode();
  }
  
  public void handleConsecutiveLag() {
    streamingModule.handleConsecutiveLag();
  }
}
