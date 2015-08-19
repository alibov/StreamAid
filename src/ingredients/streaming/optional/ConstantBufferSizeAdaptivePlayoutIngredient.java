package ingredients.streaming.optional;

import ingredients.AbstractIngredient;

import java.util.Random;

import messages.Message;
import modules.player.VideoStream;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import utils.Utils;

public class ConstantBufferSizeAdaptivePlayoutIngredient extends AbstractIngredient<PullAlgorithm> {
  final public int bufferSize;
  final public double maxSpeedChange;
  
  public ConstantBufferSizeAdaptivePlayoutIngredient(final int bufferSize, final double maxSpeedChange, final Random r) {
    super(r);
    this.bufferSize = bufferSize;
    this.maxSpeedChange = maxSpeedChange;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    final VideoStream videoStream = client.player.getVs();
    if (videoStream == null || client.isServerMode() || videoStream.isBuffering()) {
      return;
    }
    final int pullWindowSize = (int) (Math.min(alg.latestChunk, Utils.getRound()) - (videoStream.windowOffset - 1));
    if (pullWindowSize > bufferSize + 1) {
      videoStream.playSpeed = 1 + maxSpeedChange;
    } else if (pullWindowSize < bufferSize) {
      videoStream.playSpeed = 1 - maxSpeedChange;
    } else {
      videoStream.playSpeed = 1;
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
