package ingredients;

import interfaces.NodeConnectionAlgorithm;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class LogReceivedChunksIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  public LogReceivedChunksIngredient(final Random r) {
    super(r);
  }
  
  public Map<NodeAddress/* sender node */, Map<Long/* cycle */, Set<Long>/* chunks */>> receivedChunks = new TreeMap<NodeAddress, Map<Long, Set<Long>>>();
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    client.network.addListener(this, ChunkMessage.class);
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ChunkMessage) {
      final Long currentRound = Utils.getRound();
      Utils.checkExistence(receivedChunks, message.sourceId, new TreeMap<Long, Set<Long>>());
      Utils.checkExistence(receivedChunks.get(message.sourceId), currentRound, new TreeSet<Long>());
      if (message instanceof ChunkMessage) {
        final ChunkMessage cm = (ChunkMessage) message;
        receivedChunks.get(message.sourceId).get(currentRound).add(cm.chunk.index);
      }
    }
  }
}
