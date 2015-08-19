package bandits.algo;

import java.io.FileWriter;
import java.io.IOException;

import logging.ObjectLogger;
import bandits.state.StateIngredient;

public class AlgoPerState extends BanditsAlgorithm {
  FileWriter fw = null;
  double currentState;
  private final int def;
  
  public AlgoPerState(final StateIngredient state, final int def, final int optionsNum) {
    super(state, null, optionsNum);
    this.def = def;
  }
  
  int totalRoundsPlayed = 0;
  
  @Override public int playNextRound() {
    try {
      if (fw == null) {
        fw = new FileWriter(ObjectLogger.dirName + "APSstats.csv");
        fw.append("round,optionchosen,state\n");
      }
      currentState = state.getCurrentState();
      if (currentState == -1) {
        chosenArm = def;
      } else {
        chosenArm = (int) currentState % optionsNum;
      }
      fw.append(totalRoundsPlayed + "," + chosenArm + "," + currentState + "\n");
      fw.flush();
      totalRoundsPlayed++;
      return chosenArm;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
