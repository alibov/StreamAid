package bandits.reward;

import interfaces.NodeConnectionAlgorithm;

import java.util.HashSet;
import java.util.Set;

import messages.BitmapUpdateMessage;
import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import utils.Utils;

public class ChunksReceivedAvailableReward extends RewardIngredient {
  public ChunksReceivedAvailableReward() {
    super(null);
  }
  
  int roundCount;
  int chunksReceived;
  Set<Long> missingChunks = new HashSet<Long>();
  Set<Long> availableChunks = new HashSet<Long>();
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    client.network.addListener(this, ChunkMessage.class);
    // TODO tie reward to specific streaming module
    client.network.addListener(this, BitmapUpdateMessage.class);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (Utils.getMovieTime() < 0 || client.isServerMode()) {
      return;
    }
    if (client.player.getVs() != null) {
      missingChunks.addAll(client.player.getVs().getMissingChunks(0, Long.MAX_VALUE));
    }
    roundCount++;
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkMessage) {
      chunksReceived++;
    } else if (message instanceof BitmapUpdateMessage) {
      availableChunks.addAll(((BitmapUpdateMessage) message).bitmap.toIndices());
    }
  }
  
  @Override public void startNewRound() {
    roundCount = 0;
    chunksReceived = 0;
    missingChunks.clear();
    availableChunks.clear();
  }
  
  @Override public double getReward() {
    final HashSet<Long> intersection = new HashSet<Long>(missingChunks);
    intersection.retainAll(availableChunks);
    if (intersection.size() == 0) {
      // No available chunks - reward should be ignored
      return 0.0;
    }
    // TODO test
    return ((double) chunksReceived) / intersection.size();
  }
}
