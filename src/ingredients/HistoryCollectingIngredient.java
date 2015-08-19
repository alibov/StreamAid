package ingredients;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class HistoryCollectingIngredient extends AbstractIngredient<OverlayModule<?>> {
  public HistoryCollectingIngredient(final Random r) {
    super(r);
  }
  
  public final Map<NodeAddress, Integer> historyScores = new HashMap<NodeAddress, Integer>();
  public Set<NodeAddress> ignoreList = new HashSet<NodeAddress>();
  public static final int initValue = 1;
  public static final int chunkScore = 1;
  
  @Override public void setClientAndComponent(final P2PClient client, final OverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
    client.network.addListener(this, ChunkMessage.class);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    for (final NodeAddress n : alg.getNeighbors()) {
      Utils.checkExistence(historyScores, n, initValue);
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (ignoreList.contains(message.sourceId)) {
      return;
    }
    Utils.checkExistence(historyScores, message.sourceId, initValue);
    if (message instanceof ChunkMessage) {
      historyScores.put(message.sourceId, historyScores.get(message.sourceId) + chunkScore);
    }
  }
  
  public void addToScore(final NodeAddress node, final int addition) {
    Utils.checkExistence(historyScores, node, initValue);
    historyScores.put(node, historyScores.get(node) + addition);
  }
}
