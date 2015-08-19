package bandits.reward;

import messages.Message;
import utils.Utils;

public class SimpleCIReward extends RewardIngredient {
  public SimpleCIReward() {
    super(null);
  }
  
  int roundCount;
  private int lastChunksPlayed;
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (Utils.getMovieTime() < 0 || client.player.getVs() == null) {
      return;
    }
    roundCount++;
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
  
  @Override public void startNewRound() {
    roundCount = 0;
    lastChunksPlayed = client.player.getChunksPlayed();
  }
  
  @Override public double getReward() {
    return roundCount == 0 ? 0 : ((double) (client.player.getChunksPlayed() - lastChunksPlayed)) / roundCount;
  }
}
