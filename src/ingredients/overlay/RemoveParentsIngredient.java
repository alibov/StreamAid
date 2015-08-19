package ingredients.overlay;

import ingredients.AbstractIngredient;

import java.util.Random;
import java.util.TreeSet;

import logging.TextLogger;
import messages.ConnectionRequestDeclinedMessage;
import messages.Message;
import modules.overlays.MultipleTreeOverlayModule;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class RemoveParentsIngredient extends AbstractIngredient<MultipleTreeOverlayModule<?>> {
  final int fatherDropTimeout;
  int fatherDropCurr;
  private final int club;
  private final TreeSet<NodeAddress> banList;
  
  public RemoveParentsIngredient(final int fatherDropTimeout, final int club, final TreeSet<NodeAddress> banList, final Random r) {
    super(r);
    this.fatherDropTimeout = fatherDropTimeout;
    this.club = club;
    this.banList = banList;
    fatherDropCurr = fatherDropTimeout;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!client.isServerMode() && client.player.getVs() != null && !client.player.getVs().canPlay()
        && (int) (client.player.getVs().windowOffset % Common.currentConfiguration.descriptions) == club
        && !alg.getNodeGroup(alg.getDownloadGroupName(club)).isEmpty()) {
      fatherDropCurr--;
      if (fatherDropCurr <= 0) {
        TextLogger.log(client.network.getAddress(), "removing nonresponsive fathers of group " + alg.getDownloadGroupName(club)
            + "\n");
        final TreeSet<NodeAddress> toRemove = new TreeSet<NodeAddress>(alg.getNodeGroup(alg.getDownloadGroupName(club)));
        for (final NodeAddress n : toRemove) {
          client.network.send(new ConnectionRequestDeclinedMessage(alg.getMessageTag(), client.network.getAddress(), n));
        }
        banList.addAll(toRemove);
        alg.removeNeighbors(toRemove);
        fatherDropCurr = fatherDropTimeout;
      }
    } else {
      fatherDropCurr = fatherDropTimeout;
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // TODO Auto-generated method stub
  }
}
