package modules.network;

import interfaces.MessageHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;

import logging.ObjectLogger;
import logging.TextLogger;
import logging.TextLogger.State;
import logging.logObjects.BandwidthLog;
import logging.logObjects.ChurnLog;
import messages.ChunkMessage;
import messages.Message;
import modules.P2PClient;
import utils.Common;
import utils.Utils;
import experiment.frameworks.NodeAddress;

/**
 * A wrapper class for a network module in the P2P overlay this class should be
 * extended by a network module specific node.
 *
 * @author Alexander Libov
 *
 */
public abstract class NetworkModule implements FailureDetectorInterface {
  // Need to be set/updated constantly
  protected P2PClient client = null;
  public static ObjectLogger<ChurnLog> churnLogger = ObjectLogger.getLogger("churnLog");
  protected static ObjectLogger<BandwidthLog> uploadBandwidthUtilizationLog = ObjectLogger.getLogger("bandLog");
  protected NodeAddress nodeAddr = null;
  protected Map<String, List<MessageHandler>> listenersByTag = new ConcurrentSkipListMap<String, List<MessageHandler>>();
  protected Map<Class<? extends Message>, List<MessageHandler>> listenersByClass = new HashMap<Class<? extends Message>, List<MessageHandler>>();
  protected Set<Message> flaggedMessages = new TreeSet<Message>();
  private NodeAddress serverNode;
  protected boolean serverMode = false;
  public Map<NodeAddress, SendInfo> sendInfo = new HashMap<NodeAddress, SendInfo>();
  
  public NodeAddress getAddress() {
    return nodeAddr;
  }
  
  public boolean send(final Message msg) {
    final long msgSize = Utils.getSize(msg);
    Utils.checkExistence(sendInfo, msg.destID, new SendInfo());
    sendInfo.get(msg.destID).bitsSent += msgSize;
    if (!msg.isOverheadMessage()) {
      sendInfo.get(msg.destID).contentBitsSent += msgSize;
    }
    final boolean retVal = sendMessage(msg);
    if (retVal) {
      TextLogger.logMessage(msg, State.Sent);
    } else {
      TextLogger.logMessage(msg, State.Send_failed);
    }
    return retVal;
  }
  
  abstract protected boolean sendMessage(Message msg);
  
  Set<Long> chunksReceived = new HashSet<Long>();
  int vitalChunksReceived = 0;
  int duplicateChunksReceived = 0;
  
  protected void processEvent(final Message event) {
    if (event instanceof ChunkMessage) {
      if (chunksReceived.contains(((ChunkMessage) event).chunk.index)) {
        duplicateChunksReceived++;
      } else {
        chunksReceived.add(((ChunkMessage) event).chunk.index);
        vitalChunksReceived++;
      }
    }
    TextLogger.logMessage(event, State.Received);
    final long msgSize = Utils.getSize(event);
    Utils.checkExistence(sendInfo, event.sourceId, new SendInfo());
    sendInfo.get(event.sourceId).bitsReceived += msgSize;
    if (!event.isOverheadMessage()) {
      sendInfo.get(event.sourceId).contentBitsReceived += msgSize;
    }
    if (listenersByTag.get(event.tag) != null) {
      for (final MessageHandler mh : listenersByTag.get(event.tag)) {
        mh.handleMessage(event);
      }
    } else {
      System.err.println("event was received but no one was listening: " + event);
      TextLogger.log(getAddress(), "event was received but no one was listening: " + event + "\n");
      TextLogger.log(event.sourceId, "event was received but no one was listening: " + event + "\n");
    }
    if (listenersByClass.get(event.getClass()) != null) {
      for (final MessageHandler mh : listenersByClass.get(event.getClass())) {
        mh.handleMessage(event);
      }
    }
  }
  
  public void addListener(final MessageHandler handler, final String tag) {
    Utils.checkExistence(listenersByTag, tag, new LinkedList<MessageHandler>());
    listenersByTag.get(tag).add(handler);
  }
  
  public void removeListener(final MessageHandler handler, final String tag) {
    Utils.checkExistence(listenersByTag, tag, new LinkedList<MessageHandler>());
    listenersByTag.get(tag).remove(handler);
    if (listenersByTag.get(tag).isEmpty()) {
      listenersByTag.remove(tag);
    }
  }
  
  public void addListener(final MessageHandler handler, final Class<? extends Message> clasz) {
    Utils.checkExistence(listenersByClass, clasz, new LinkedList<MessageHandler>());
    listenersByClass.get(clasz).add(handler);
  }
  
  public abstract long getUploadBandwidth();
  
  public abstract long getEstimatedLatency(NodeAddress adr);
  
  @Override public String toString() {
    return "NetworkNode " + nodeAddr;
  }
  
  public void setServerNode(final NodeAddress adr) {
    serverNode = adr;
  }
  
  public NodeAddress getServerNode() {
    return serverNode;
  }
  
  public void setServerMode() {
    serverMode = true;
  }
  
  public boolean isServerMode() {
    return serverMode;
  }
  
  protected void init() {
    if (client != null) {
      throw new RuntimeException("init called twice for " + nodeAddr);
    }
    TextLogger.log(nodeAddr, "joined the system\n");
    client = new P2PClient(this, new Random(Common.currentConfiguration.experimentRandom.nextLong()));
    churnLogger.logObject(new ChurnLog(nodeAddr.toString(), true, client.player.startupBuffering,
        client.player.bufferFromFirstChunk));
  }
  
  public class SendInfo {
    public long bitsSent = 0;
    public long bitsReceived = 0;
    public long contentBitsSent = 0;
    public long contentBitsReceived = 0;
    public long time = 1;
  }
  
  public int getVitalChunksReceived() {
    return vitalChunksReceived;
  }
  
  public int getDuplicateChunksReceived() {
    return duplicateChunksReceived;
  }
  
  public void flagMessage(final Message message) {
    flaggedMessages.add(message);
  }
}
