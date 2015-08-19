package bandits.algo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import logging.ObjectLogger;
import bandits.reward.RewardIngredient;

public class EpsilonGreedy extends BanditsAlgorithm {
  FileWriter fw = null;
  
  public interface EpsilonFunc {
    double getEpsilon(int roundNumber);
  }
  
  public static class ConstantEpsilon implements EpsilonFunc {
    double epsilon;
    
    public ConstantEpsilon(final double epsilon) {
      this.epsilon = epsilon;
    }
    
    @Override public double getEpsilon(final int roundNumber) {
      return epsilon;
    }
  }
  
  public static class LinearEpsilon implements EpsilonFunc {
    double coef;
    
    public LinearEpsilon(final double coef) {
      this.coef = coef;
    }
    
    @Override public double getEpsilon(final int roundNumber) {
      return 1 - coef * roundNumber;
    }
  }
  
  public static class InvLinearEpsilon implements EpsilonFunc {
    double coef;
    
    public InvLinearEpsilon(final double coef) {
      this.coef = coef;
    }
    
    @Override public double getEpsilon(final int roundNumber) {
      return coef / roundNumber;
    }
  }
  
  public final EpsilonFunc epsilonFunc;
  int roundsPlayed = 0;
  Random r;
  ArrayList<Double> totalReward = new ArrayList<Double>();
  ArrayList<Integer> timesChosen = new ArrayList<Integer>();
  
  public EpsilonGreedy(final Random r, final RewardIngredient reward, final int optionsNum, final EpsilonFunc epsilonFunc) {
    super(null, reward, optionsNum);
    this.r = r;
    for (int i = 0; i < optionsNum; ++i) {
      totalReward.add(0.0);
      timesChosen.add(0);
    }
    this.epsilonFunc = epsilonFunc;
  }
  
  @Override public int playNextRound() {
    try {
      if (fw == null) {
        fw = new FileWriter(ObjectLogger.dirName + "EGstats.csv");
        fw.append("round,option,reward,");
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("option" + i + "score,");
        }
        fw.append("optionchosen,epsilon\n");
      }
      if (roundsPlayed > 0) {
        final double rreward = reward.getReward();
        fw.append(roundsPlayed + "," + chosenArm + "," + rreward + ",");
        timesChosen.set(chosenArm, timesChosen.get(chosenArm) + 1);
        totalReward.set(chosenArm, totalReward.get(chosenArm) + rreward);
      } else {
        fw.append("0,-1,-1,");
      }
      reward.startNewRound();
      roundsPlayed++;
      if (roundsPlayed == 1 || r.nextDouble() <= epsilonFunc.getEpsilon(roundsPlayed)) {
        chosenArm = r.nextInt(optionsNum);
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("-1,");
        }
      } else {
        chosenArm = getBestArm();
      }
      fw.append(chosenArm + "," + epsilonFunc.getEpsilon(roundsPlayed) + "\n");
      fw.flush();
      return chosenArm;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private int getBestArm() throws IOException {
    int best = 0;
    double bestScore = 0.0;
    for (int i = 0; i < totalReward.size(); ++i) {
      if (timesChosen.get(i) == 0) {
        fw.append("-1,");
        continue;
      }
      final double score = totalReward.get(i) / timesChosen.get(i);
      fw.append(score + ",");
      if (score > bestScore) {
        bestScore = score;
        best = i;
      }
    }
    return best;
  }
}
