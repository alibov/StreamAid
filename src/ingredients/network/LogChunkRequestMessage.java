package ingredients.network;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkRequestMessage;
import messages.Message;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class LogChunkRequestMessage extends AbstractIngredient<NodeConnectionAlgorithm> {
  public final int recordsToSave;
  public Map<NodeAddress/* sender */, Map<NodeAddress/* receiver */, Map<Long/* round */, Set<Long>/* requested
                                                                                                    * chunks */>>> sentRequests = new TreeMap<NodeAddress, Map<NodeAddress, Map<Long, Set<Long>>>>();
  
  public LogChunkRequestMessage(final int recordsToSave, final Random r) {
    super(r);
    this.recordsToSave = recordsToSave;
  }
  
  // pointcut chunkRequest(Message m): args(m)&&(call(boolean
  // interfaces.NetworkNode.send(ChunkRequestMessage)));
  // after(Message m) returning: chunkRequest(m){
  @Override public void handleMessage(final Message message) {
    // TODO make sure that the message is sent!
    if (!(message instanceof ChunkRequestMessage)) {
      return;
    }
    final ChunkRequestMessage m = (ChunkRequestMessage) message;
    final long currentRound = Utils.getRound();
    Utils.checkExistence(sentRequests, m.sourceId, new TreeMap<NodeAddress, Map<Long, Set<Long>>>());
    Utils.checkExistence(sentRequests.get(m.sourceId), m.destID, new TreeMap<Long, Set<Long>>());
    if (sentRequests.get(m.sourceId).get(m.destID).get(currentRound) == null) {
      sentRequests.get(m.sourceId).get(m.destID).put(currentRound, new TreeSet<Long>());
      Utils.retainOnlyNewest(recordsToSave, sentRequests.get(m.sourceId).get(m.destID));
    }
    sentRequests.get(m.sourceId).get(m.destID).get(currentRound).addAll(m.chunks);
  }
}
