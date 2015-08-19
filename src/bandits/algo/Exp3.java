package bandits.algo;

import java.util.ArrayList;
import java.util.Random;

import bandits.reward.RewardIngredient;
import bandits.state.StateIngredient;

public class Exp3 extends BanditsAlgorithm {
  double gamma = 0.06;
  int t = 0;
  ArrayList<Double> weights;
  final ArrayList<Double> probabilities;
  Random r;
  
  public Exp3(final Random r, final StateIngredient state, final RewardIngredient reward, final int optionsNum) {
    super(state, reward, optionsNum);
    this.r = r;
    weights = new ArrayList<Double>(optionsNum);
    probabilities = new ArrayList<Double>(optionsNum);
    for (int i = 0; i < optionsNum; ++i) {
      weights.add(1.0);
      probabilities.add(1.0 / optionsNum);
    }
  }
  
  @Override public int playNextRound() {
    if (t > 0) {
      updateWeights();
    }
    reward.startNewRound();
    ++t;
    return chosenArm = getWeightedArm();
  }
  
  void updateProbabilities() {
    probabilities.clear();
    Double sum = 0.0;
    for (final Double w : weights) {
      sum += w;
    }
    for (final Double w : weights) {
      probabilities.add((1 - gamma) * w / sum + gamma / weights.size());
    }
  }
  
  private int getWeightedArm() {
    final double choice = r.nextDouble();
    Double sum = 0.0;
    for (int i = 0; i < probabilities.size(); ++i) {
      sum += probabilities.get(i);
      if (choice <= sum) {
        return i;
      }
    }
    throw new RuntimeException("shouldn't get here");
  }
  
  private void updateWeights() {
    final double estimatedReward = reward.getReward() / probabilities.get(chosenArm);
    weights.set(chosenArm, weights.get(chosenArm) * Math.exp(estimatedReward * gamma / weights.size()));
    if (weights.get(chosenArm).isNaN()) {
      throw new RuntimeException("illegal computation");
    }
    updateProbabilities();
  }
}
