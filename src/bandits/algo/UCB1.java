package bandits.algo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import logging.ObjectLogger;
import bandits.reward.RewardIngredient;

public class UCB1 extends BanditsAlgorithm {
  FileWriter fw = null;
  int roundsPlayed = 0;
  ArrayList<Double> totalReward = new ArrayList<Double>();
  ArrayList<Integer> timesChosen = new ArrayList<Integer>();
  private final int conv;
  
  public UCB1(final RewardIngredient reward, final int optionsNum, final int conv) {
    super(null, reward, optionsNum);
    for (int i = 0; i < optionsNum; ++i) {
      totalReward.add(0.0);
      timesChosen.add(0);
    }
    this.conv = conv;
  }
  
  @Override public int playNextRound() {
    try {
      if (fw == null) {
        fw = new FileWriter(ObjectLogger.dirName + "UCB1stats.csv");
        fw.append("round,option,reward,");
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("option" + i + "score,");
        }
        fw.append("optionchosen\n");
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
      if (roundsPlayed < optionsNum) {
        chosenArm = roundsPlayed;
        for (int i = 0; i < optionsNum; ++i) {
          fw.append("0,");
        }
      } else {
        chosenArm = getBestArm();
      }
      fw.append(chosenArm + "\n");
      fw.flush();
      roundsPlayed++;
      return chosenArm;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  double upperBound(final int step, final int numPlays) {
    return Math.sqrt(2 * Math.log(step + 1) / numPlays);
  }
  
  private int getBestArm() throws IOException {
    int best = 0;
    double bestScore = 0.0;
    for (int i = 0; i < totalReward.size(); ++i) {
      final double score = (totalReward.get(i) / timesChosen.get(i)) + upperBound(roundsPlayed + conv, timesChosen.get(i) + conv);
      fw.append(score + ",");
      if (score > bestScore) {
        bestScore = score;
        best = i;
      }
    }
    return best;
  }
  
  @Override protected void finalize() throws Throwable {
    super.finalize();
    if (fw != null) {
      fw.close();
    }
  }
}
