package bandits.state;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

public abstract class StateIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  public StateIngredient(final Random r) {
    super(r);
  }
  
  public abstract double getCurrentState();
}
