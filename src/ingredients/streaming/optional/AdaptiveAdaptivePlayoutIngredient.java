package ingredients.streaming.optional;

import ingredients.AbstractIngredient;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import messages.Message;
import modules.player.VideoStream;
import modules.streaming.pullAlgorithms.PullAlgorithm;

public class AdaptiveAdaptivePlayoutIngredient extends AbstractIngredient<PullAlgorithm> {
  final public int bufferSize;
  final public double maxSpeedChange;
  Set<Long> lastRoundMissingChunks = new HashSet<Long>();
  int playbackSeconds = 0;
  long startingAverage = 0;
  double EMA;
  double alpha = 0.5;
  int initRounds = 5;
  
  public AdaptiveAdaptivePlayoutIngredient(final int bufferSize, final double maxSpeedChange, final int initRounds,
      final double alpha, final Random r) {
    super(r);
    this.bufferSize = bufferSize;
    this.maxSpeedChange = maxSpeedChange;
    this.initRounds = initRounds;
    this.alpha = alpha;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    final VideoStream videoStream = client.player.getVs();
    if (videoStream == null || client.isServerMode() || videoStream.isBuffering()) {
      return;
    }
    final Set<Long> currMissing = alg.getMissingChunks();
    lastRoundMissingChunks.removeAll(currMissing);
    // now last contains received chunks
    if (!lastRoundMissingChunks.isEmpty()) {
      addToAverage(Collections.min(lastRoundMissingChunks) - client.player.getVs().windowOffset);
      performSpeedChange();
    }
    playbackSeconds++;
    lastRoundMissingChunks = currMissing;
  }
  
  private void performSpeedChange() {
    if (playbackSeconds < initRounds) {
      return;
    }
    final VideoStream videoStream = client.player.getVs();
    if (EMA > bufferSize + 1) {
      videoStream.playSpeed = 1 + maxSpeedChange;
    } else if (EMA < bufferSize) {
      videoStream.playSpeed = 1 - maxSpeedChange;
    } else {
      videoStream.playSpeed = 1;
    }
  }
  
  private void addToAverage(final long distanceFromOffset) {
    if (playbackSeconds < initRounds) {
      startingAverage += distanceFromOffset;
      if (playbackSeconds == initRounds - 1) {
        startingAverage /= initRounds;
        EMA = startingAverage;
      }
      return;
    }
    EMA = alpha * distanceFromOffset + (1 - alpha) * EMA;
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
