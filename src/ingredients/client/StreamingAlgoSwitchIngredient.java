package ingredients.client;

import ingredients.AbstractIngredient;

import java.util.ArrayList;
import java.util.Random;

import logging.ObjectLogger;
import logging.TextLogger;
import logging.logObjects.OptionLog;
import messages.Message;
import modules.P2PClient;
import modules.streaming.PushPull;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import utils.Utils;
import bandits.algo.BanditsAlgorithm;
import experiment.ExperimentConfiguration.GetStreamingMod;

public class StreamingAlgoSwitchIngredient extends AbstractIngredient<PushPull> {
  int currentPeriod = 0;
  final int timeout;
  int currTimeout = 1;
  int chosenOption = -1;
  private final BanditsAlgorithm banditsAlgo;
  private static ObjectLogger<OptionLog> optionLogger = ObjectLogger.getLogger("puloptlog");
  private final ArrayList<GetStreamingMod> options;
  
  @Override public void setClientAndComponent(final P2PClient client, final PushPull alg) {
    super.setClientAndComponent(client, alg);
  }
  
  public StreamingAlgoSwitchIngredient(final ArrayList<GetStreamingMod> options, final BanditsAlgorithm banditsAlgo,
      final int timeout, final Random r) {
    super(r);
    this.timeout = timeout;
    this.options = options;
    this.banditsAlgo = banditsAlgo;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (Utils.getMovieTime() < 0) {
      return;
    }
    currTimeout--;
    if (currTimeout == 0) {
      currentPeriod++;
      currTimeout = timeout;
      if (currentPeriod > 1) {
        TextLogger.log(client.network.getAddress(), "BANDITS> chosen alg: " + banditsAlgo.chosenArm + " reward: "
            + banditsAlgo.reward.getReward() + "\n");
      }
      chosenOption = banditsAlgo.playNextRound();
      alg.pullAlg.setLatestChunk(0);
      alg.pullAlg.mainOverlay = null;
      alg.pullAlg.deactivate();
      final PullAlgorithm nextStr = (PullAlgorithm) options.get(chosenOption).getAlg(client);
      alg.pullAlg = nextStr;
    }
    optionLogger.logObject(new OptionLog(client.network.getAddress().toString(), currentPeriod, chosenOption));
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
