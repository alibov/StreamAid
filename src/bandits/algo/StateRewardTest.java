package bandits.algo;

import java.util.TreeMap;

import utils.Utils;
import bandits.reward.RewardIngredient;
import bandits.state.StateIngredient;

public class StateRewardTest extends BanditsAlgorithm {
  int roundsPlayed = 0;
  double prevState;
  TreeMap<Double, Double> stateReward = new TreeMap<Double, Double>();
  TreeMap<Double, Integer> stateNum = new TreeMap<Double, Integer>();
  
  public StateRewardTest(final StateIngredient state, final RewardIngredient reward, final int optionsNum) {
    super(state, reward, optionsNum);
  }
  
  @Override public int playNextRound() {
    if (roundsPlayed == 0) {
      prevState = state.getCurrentState();
      roundsPlayed++;
      reward.startNewRound();
      return 0;
    }
    final double rew = reward.getReward();
    Utils.checkExistence(stateNum, prevState, 0);
    Utils.checkExistence(stateReward, prevState, 0.0);
    stateReward.put(prevState, (stateReward.get(prevState) * stateNum.get(prevState) + rew) / (stateNum.get(prevState) + 1));
    stateNum.put(prevState, stateNum.get(prevState) + 1);
    prevState = state.getCurrentState();
    roundsPlayed++;
    if (stateReward.size() > 1) {
      performCheck();
    }
    reward.startNewRound();
    return 0;
  }
  
  private void performCheck() {
    double min = Double.MAX_VALUE;
    double max = 0.0;
    for (final Double i : stateReward.keySet()) {
      for (final Double j : stateReward.keySet()) {
        if (j <= i) {
          continue;
        }
        final double alpha = Math.abs((stateReward.get(i) - stateReward.get(j)) / (j - i));
        if (alpha > max) {
          max = alpha;
        }
        if (alpha < min) {
          min = alpha;
        }
      }
    }
    if (max > 5) {
      System.out.println("min: " + min + ", max: " + max);
    }
  }
}
