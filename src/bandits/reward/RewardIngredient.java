package bandits.reward;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

public abstract class RewardIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  public RewardIngredient(final Random r) {
    super(r);
  }
  
  public abstract void startNewRound();
  
  public abstract double getReward();
}
