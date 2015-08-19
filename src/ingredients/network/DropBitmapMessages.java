package ingredients.network;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.BitmapUpdateMessage;
import messages.Message;

public class DropBitmapMessages extends AbstractIngredient<NodeConnectionAlgorithm> {
  public final double dropRate;
  
  public DropBitmapMessages(final double dropRate, final Random r) {
    super(r);
    this.dropRate = dropRate;
  }
  
  // pointcut sendBitmap(Message m,NetworkNode node): args(m) && target(node) &&
  // (call(boolean interfaces.NetworkNode.send(BitmapUpdateMessage)));
  // boolean around(Message m,NetworkNode node): sendBitmap(m,node) {
  @Override public void handleMessage(final Message message) {
    if (!(message instanceof BitmapUpdateMessage)) {
      return;
    }
    if (r.nextDouble() < dropRate) {
      client.network.flagMessage(message);
    }
  }
}
