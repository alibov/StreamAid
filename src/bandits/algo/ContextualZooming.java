package bandits.algo;

import java.util.ArrayList;
import java.util.Random;

import bandits.reward.RewardIngredient;
import bandits.state.StateIngredient;

public class ContextualZooming extends BanditsAlgorithm {
  public class StateOption {
    public double _state;
    public int option;
    
    public StateOption(final double state, final int option) {
      _state = state;
      this.option = option;
    }
  }
  
  public interface Context {
    double getDistance(StateOption a, StateOption b);
  }
  
  public class StateLinearContext implements Context {
    double alpha;
    
    public StateLinearContext(final double alpha) {
      this.alpha = alpha;
    }
    
    @Override public double getDistance(final StateOption a, final StateOption b) {
      if (a.option != b.option) {
        return 1;
      }
      return Math.abs(a._state - b._state) * alpha;
    }
  }
  
  class Ball {
    public StateOption center;
    public double radius;
    public double totalReward;
    public int timesSelected;
    
    public Ball(final StateOption center, final double radius, final double totalReward, final int timesSelected) {
      this.center = center;
      this.radius = radius;
      this.totalReward = totalReward;
      this.timesSelected = timesSelected;
    }
    
    public double getAveragePayoff() {
      return totalReward / (Math.max(1, timesSelected));
    }
  }
  
  int chosenBall;
  ArrayList<Ball> activeBalls = new ArrayList<ContextualZooming.Ball>();
  int roundsPlayed = 0;
  Random r;
  Context context = new StateLinearContext(1);
  private final int numberOfRounds;
  
  public ContextualZooming(final Random r, final StateIngredient state, final RewardIngredient reward, final int optionsNum,
      final int numberOfRounds) {
    super(state, reward, optionsNum);
    this.r = r;
    this.numberOfRounds = numberOfRounds;
    activeBalls.add(new Ball(new StateOption(0.0, r.nextInt(optionsNum)), 1.0, 0, 0));
  }
  
  @Override public int playNextRound() {
    double currentState = 0.0;
    if (roundsPlayed > 0) {
      currentState = state.getCurrentState();
      activeBalls.get(chosenBall).timesSelected++;
      activeBalls.get(chosenBall).totalReward += reward.getReward();
    }
    reward.startNewRound();
    double maxIndex = 0.0;
    for (int y = 0; y < optionsNum; ++y) {
      double minRadius = 2.0;
      int ballIndex = -1;
      for (int b = 1; b < activeBalls.size(); ++b) {
        final Ball ball = activeBalls.get(b);
        if (context.getDistance(new StateOption(currentState, y), ball.center) > ball.radius) {
          continue;
        }
        if (ball.radius < minRadius) {
          minRadius = ball.radius;
          ballIndex = b;
        }
      }
      final double index = calcIndex(activeBalls.get(ballIndex));
      if (index > maxIndex) {
        maxIndex = index;
        chosenBall = ballIndex;
      }
    }
    final Ball chosen = activeBalls.get(chosenBall);
    if (rad(chosen.timesSelected + 1) <= chosen.radius) {
      activeBalls.add(new Ball(new StateOption(currentState, chosen.center.option), chosen.radius, 0, 0));
    }
    roundsPlayed++;
    return chosen.center.option;
  }
  
  private double calcIndex(final Ball ball) {
    double retVal = Double.MAX_VALUE;
    for (final Ball b : activeBalls) {
      if (b.radius < ball.radius) {
        continue;
      }
      final double cand = Itpre(b) + context.getDistance(b.center, ball.center);
      if (cand < retVal) {
        retVal = cand;
      }
    }
    return retVal;
  }
  
  private double Itpre(final Ball ball) {
    return ball.getAveragePayoff() + 2 * ball.radius + rad(ball.timesSelected);
  }
  
  private double rad(final int num) {
    return 4 * Math.sqrt(Math.log(numberOfRounds) / (1 + num));
  }
}
