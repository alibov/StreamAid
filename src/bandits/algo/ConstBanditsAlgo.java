package bandits.algo;


public class ConstBanditsAlgo extends BanditsAlgorithm {
  public ConstBanditsAlgo(final int option) {
    super(null, null, option);
  }
  
  @Override public int playNextRound() {
    return optionsNum;
  }
}
