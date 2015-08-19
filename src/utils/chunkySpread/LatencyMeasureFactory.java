package utils.chunkySpread;

public class LatencyMeasureFactory {
  private final int windowSize;
  private final double fastPercentage;
  private final double slowPercentage;
  private final int treesNum;
  private final double highScore;
  private final double lowScore;
  
  public LatencyMeasureFactory(final int _windowSize, final int _treesNum, final double _fastPercentage,
      final double _slowPercentage, final double _highScore, final double _lowScore) {
    windowSize = _windowSize;
    fastPercentage = _fastPercentage;
    slowPercentage = _slowPercentage;
    treesNum = _treesNum;
    highScore = _highScore;
    lowScore = _lowScore;
  }
  
  public LatencyMeasure create() {
    return new LatencyMeasure(windowSize, treesNum, fastPercentage, slowPercentage, highScore, lowScore);
  }
}
