package bandits.algo;

import bandits.reward.RewardIngredient;
import bandits.state.StateIngredient;

public abstract class BanditsAlgorithm {
  public final StateIngredient state;
  public final RewardIngredient reward;
  public final int optionsNum;
  public int chosenArm;
  
  public BanditsAlgorithm(final StateIngredient state, final RewardIngredient reward, final int optionsNum) {
    this.state = state;
    this.reward = reward;
    this.optionsNum = optionsNum;
  }
  
  public abstract int playNextRound();
}
