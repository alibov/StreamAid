package ingredients.streaming;

import ingredients.AbstractIngredient;
import ingredients.streaming.mandatory.AbstractChunkRequestIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.ArrayList;
import java.util.Random;

import logging.ObjectLogger;
import logging.TextLogger;
import logging.logObjects.OptionLog;
import messages.Message;
import modules.P2PClient;
import modules.streaming.pullAlgorithms.PullAlgorithm;

import org.w3c.dom.Node;

import utils.Utils;
import bandits.algo.BanditsAlgorithm;
import experiment.StreamingModuleFactory;

public class ChunkRequestAlgoSwitchIngredient extends AbstractIngredient<PullAlgorithm> {
  int currentPeriod = 0;
  final int timeout;
  int currTimeout = 1;
  int chosenOption = -1;
  private final BanditsAlgorithm banditsAlgo;
  private static ObjectLogger<OptionLog> optionLogger = ObjectLogger.getLogger("puloptlog");
  private final ArrayList<Node> options;
  
  @Override public void setClientAndComponent(final P2PClient client, final PullAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    alg.addIngredient(banditsAlgo.reward, client);
    if (banditsAlgo.state != null) {
      alg.addIngredient(banditsAlgo.state, client);
    }
  }
  
  public ChunkRequestAlgoSwitchIngredient(final ArrayList<Node> options, final BanditsAlgorithm banditsAlgo, final int timeout,
      final Random r) {
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
      alg.getIngredient(AbstractChunkRequestIngredient.class).deactivate();
      chosenOption = banditsAlgo.playNextRound();
      final AbstractIngredient<? extends NodeConnectionAlgorithm> chosenIngredient = StreamingModuleFactory.initIngredient(alg,
          options.get(chosenOption), r, client);
      if (client.isServerMode()) {
        chosenIngredient.setServerMode();
      }
    }
    optionLogger.logObject(new OptionLog(client.network.getAddress().toString(), currentPeriod, chosenOption));
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
