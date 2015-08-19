package ingredients.streaming.optional;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.Message;
import modules.player.VideoStream;

public class ConstantLagAdaptivePlayoutIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  final public int minTargetLag;
  final public double maxSpeedChange;
  final public int maxTargetLag;
  
  public ConstantLagAdaptivePlayoutIngredient(final int minTargetLag, final int maxTargetLag, final double maxSpeedChange,
      final Random r) {
    super(r);
    this.minTargetLag = minTargetLag;
    this.maxTargetLag = maxTargetLag;
    this.maxSpeedChange = maxSpeedChange;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    final VideoStream videoStream = client.player.getVs();
    if (videoStream == null || client.isServerMode() || videoStream.isBuffering()) {
      return;
    }
    final double lag = videoStream.getLag();
    if (lag > maxTargetLag) {
      videoStream.playSpeed = 1 + maxSpeedChange;
    } else if (lag < minTargetLag) {
      videoStream.playSpeed = 1 - maxSpeedChange;
    } else {
      videoStream.playSpeed = 1;
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
