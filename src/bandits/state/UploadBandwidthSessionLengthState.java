package bandits.state;

import ingredients.overlay.InformationExchange;
import ingredients.overlay.InformationExchange.InfoType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.InfoMessage;
import messages.Message;
import modules.streaming.AdaptiveStreaming;
import modules.streaming.PushPull;
import modules.streaming.StreamingModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class UploadBandwidthSessionLengthState extends StateIngredient {
  static final int sessionLengthretention = 40;
  static final int uploadBWretention = 0;
  static final long lengthBorder = 100;
  static final long uploadBorder = 6000000;
  boolean sentUploadInfo = false;
  Map<NodeAddress, Long> uploadBW = new HashMap<NodeAddress, Long>();
  Map<NodeAddress, Long> sessionLength = new HashMap<NodeAddress, Long>();
  Map<NodeAddress, Long> downRounds = new HashMap<NodeAddress, Long>();
  private double currentState = -1;
  private int rounds = 0;
  
  public UploadBandwidthSessionLengthState() {
    super(null);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    sentUploadInfo = true;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    rounds++;
    if (!sentUploadInfo) {
      final Map<InfoType, Object> info = new HashMap<InformationExchange.InfoType, Object>();
      info.put(InfoType.UPLOADBANDWIDTHPERNEIGHBOR, client.network.getUploadBandwidth());
      client.network.send(new InfoMessage(getMessageTag(), client.network.getAddress(), client.network.getServerNode(), info));
      sentUploadInfo = true;
    }
    if (!client.isServerMode()) {
      return;
    }
    if (sessionLength.isEmpty()) {
      currentState = -1;
      return;
    }
    final Set<NodeAddress> sessionLengthToRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : sessionLength.keySet()) {
      if (client.network.isUp(n)) {
        sessionLength.put(n, sessionLength.get(n) + 1);
      } else {
        Utils.checkExistence(downRounds, n, 0L);
        downRounds.put(n, downRounds.get(n) + 1);
        if (downRounds.get(n) > uploadBWretention) {
          uploadBW.remove(n);
        }
        if (downRounds.get(n) > sessionLengthretention) {
          sessionLengthToRemove.add(n);
        }
        if (downRounds.get(n) > Math.max(sessionLengthretention, uploadBWretention)) {
          downRounds.remove(n);
        }
      }
    }
    sessionLength.keySet().removeAll(sessionLengthToRemove);
    final long averageLength = Utils.averageLong(sessionLength.values());
    final long averageUpload = Utils.averageLong(uploadBW.values());
    currentState = (averageLength < Math.min(lengthBorder, rounds / 2) ? 2.0 : 0) + (averageUpload < uploadBorder ? 1.0 : 0);
    final StreamingModule sm = ((AdaptiveStreaming) alg).streamingModules.getLast();
    System.out.println("round: " + rounds + ", averageLength: " + averageLength + ", averageUpload: " + averageUpload + ", state: "
        + currentState + ", algo: " + sm.getMessageTag() + "-"
        + (sm instanceof PushPull ? ((PushPull) sm).pullAlg.mainOverlay.getMessageTag() : sm.mainOverlay.getMessageTag()));
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof InfoMessage) {
      uploadBW.put(message.sourceId, (Long) ((InfoMessage) message).info.get(InfoType.UPLOADBANDWIDTHPERNEIGHBOR));
      sessionLength.put(message.sourceId, 0L);
    }
  }
  
  @Override public double getCurrentState() {
    return currentState;
  }
}
