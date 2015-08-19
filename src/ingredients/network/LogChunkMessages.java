package ingredients.network;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkMessage;
import messages.Message;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class LogChunkMessages extends AbstractIngredient<NodeConnectionAlgorithm> {
  public final int recordsToSave;
  
  public LogChunkMessages(final int recordsToSave, final Random r) {
    super(r);
    this.recordsToSave = recordsToSave;
  }
  
  public Map<NodeAddress, Map<NodeAddress, Map<Long, Set<Long>>>> sentChunks = new TreeMap<NodeAddress, Map<NodeAddress, Map<Long, Set<Long>>>>();
  
  // pointcut chunkSend(final Message m): args(m) && call(boolean
  // interfaces.NetworkNode.send(ChunkMessage)) ;
  // after(final Message m) returning (final boolean b): chunkSend(m){
  @Override public void handleMessage(final Message message) {
    // TODO make sure that the message is sent!
    if (!(message instanceof ChunkMessage)) {
      return;
    }
    final ChunkMessage m = (ChunkMessage) message;
    final long round = Utils.getRound();
    Utils.checkExistence(sentChunks, m.sourceId, new TreeMap<NodeAddress, Map<Long, Set<Long>>>());
    Utils.checkExistence(sentChunks.get(m.sourceId), m.destID, new TreeMap<Long, Set<Long>>());
    if (sentChunks.get(m.sourceId).get(m.destID).get(round) == null) {
      sentChunks.get(m.sourceId).get(m.destID).put(round, new TreeSet<Long>());
      Utils.retainOnlyNewest(recordsToSave, sentChunks.get(m.sourceId).get(m.destID));
    }
    sentChunks.get(m.sourceId).get(m.destID).get(round).add(m.chunk.index);
  }
}
