package bandits.algo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import logging.ObjectLogger;
import utils.Utils;
import bandits.reward.RewardIngredient;
import bandits.state.StateIngredient;

public class StatefullEpsilonGreedy extends BanditsAlgorithm {
  FileWriter fw = null;
  
  interface EpsilonFunc {
    double getEpsilon(int roundNumber);
  }
  
  static class ConstantEpsilon implements EpsilonFunc {
    double epsilon;
    
    public ConstantEpsilon(final double epsilon) {
      this.epsilon = epsilon;
    }
    
    @Override public double getEpsilon(final int roundNumber) {
      return epsilon;
    }
  }
  
  static class LinearEpsilon implements EpsilonFunc {
    double base;
    
    public LinearEpsilon(final double base) {
      this.base = base;
    }
    
    @Override public double getEpsilon(final int roundNumber) {
      return base / roundNumber;
    }
  }
  
  EpsilonFunc epsilonFunc = new LinearEpsilon(1);
  // EpsilonFunc epsilonFunc = new ConstantEpsilon(0.2);
  int totalRoundsPlayed = 0;
  Random r;
  Map<Double, ArrayList<Double>> totalReward = new HashMap<Double, ArrayList<Double>>();
  Map<Double, ArrayList<Integer>> timesChosen = new HashMap<Double, ArrayList<Integer>>();
  Map<Double, Integer> roundsPlayed = new HashMap<Double, Integer>();
  double currentState;
  
  public StatefullEpsilonGreedy(final Random r, final StateIngredient state, final RewardIngredient reward, final int optionsNum) {
    super(state, reward, optionsNum);
    this.r = r;
  }
  
  @Override public int playNextRound() {
    try {
      if (fw == null) {
        fw = new FileWriter(ObjectLogger.dirName + "SEGstats.csv");
        fw.append("round,option,reward,");
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("option" + i + "score,");
        }
        fw.append("optionchosen,epsilon,state\n");
      }
      if (totalRoundsPlayed > 0) {
        final double rreward = reward.getReward();
        fw.append(totalRoundsPlayed + "," + chosenArm + "," + rreward + ",");
        ArrayList<Double> sReward;
        ArrayList<Integer> sChosen;
        if (!totalReward.containsKey(currentState)) {
          sReward = new ArrayList<Double>();
          sChosen = new ArrayList<Integer>();
          for (int i = 0; i < optionsNum; ++i) {
            sReward.add(0.0);
            sChosen.add(0);
          }
          totalReward.put(currentState, sReward);
          timesChosen.put(currentState, sChosen);
        } else {
          sReward = totalReward.get(currentState);
          sChosen = timesChosen.get(currentState);
        }
        sChosen.set(chosenArm, sChosen.get(chosenArm) + 1);
        sReward.set(chosenArm, sReward.get(chosenArm) + rreward);
      } else {
        fw.append("0,-1,-1,");
      }
      totalRoundsPlayed++;
      currentState = state.getCurrentState();
      Utils.checkExistence(roundsPlayed, currentState, 0);
      roundsPlayed.put(currentState, roundsPlayed.get(currentState) + 1);
      reward.startNewRound();
      if (roundsPlayed.get(currentState) == 1 || r.nextDouble() <= epsilonFunc.getEpsilon(roundsPlayed.get(currentState))) {
        chosenArm = r.nextInt(optionsNum);
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("-1,");
        }
      } else {
        chosenArm = getBestArm();
      }
      fw.append(chosenArm + "," + epsilonFunc.getEpsilon(roundsPlayed.get(currentState)) + "," + currentState + "\n");
      fw.flush();
      return chosenArm;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private int getBestArm() throws IOException {
    int best = 0;
    double bestScore = 0.0;
    for (int i = 0; i < totalReward.get(currentState).size(); ++i) {
      if (timesChosen.get(currentState).get(i) == 0) {
        fw.append("-1,");
        continue;
      }
      final double score = totalReward.get(currentState).get(i) / timesChosen.get(currentState).get(i);
      fw.append(score + ",");
      if (score > bestScore) {
        bestScore = score;
        best = i;
      }
    }
    return best;
  }
}
