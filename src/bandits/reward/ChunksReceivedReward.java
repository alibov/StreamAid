package bandits.reward;

import interfaces.NodeConnectionAlgorithm;
import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import utils.Utils;

public class ChunksReceivedReward extends RewardIngredient {
  public ChunksReceivedReward() {
    super(null);
  }
  
  int roundCount;
  int chunksReceived;
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    client.network.addListener(this, ChunkMessage.class);
    // TODO tie reward to specific streaming module
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (Utils.getMovieTime() < 0) {
      return;
    }
    roundCount++;
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkMessage) {
      chunksReceived++;
    }
  }
  
  @Override public void startNewRound() {
    roundCount = 0;
    chunksReceived = 0;
  }
  
  @Override public double getReward() {
    if (roundCount == 0) {
      return 0;
    }
    return ((double) chunksReceived) / roundCount;
  }
}
