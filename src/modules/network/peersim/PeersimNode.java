package modules.network.peersim;

import java.util.LinkedList;
import java.util.Queue;

import logging.AggregateObjectLogger;
import logging.ObjectLogger;
import logging.TextLogger;
import logging.logObjects.BandwidthLog;
import logging.logObjects.ChunkSendLog;
import logging.logObjects.ChurnLog;
import logging.logObjects.SendLog;
import messages.ChunkMessage;
import messages.Message;
import modules.network.NetworkModule;
import modules.player.ServerVideoStream;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.Cleanable;
import peersim.core.Fallible;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;
import utils.Common;
import utils.Utils;
import entites.NodeAvailability;
import experiment.frameworks.NodeAddress;

public class PeersimNode extends NetworkModule implements CDProtocol, EDProtocol<Message>, Cleanable {
  protected static ObjectLogger<SendLog> sendLogger = new AggregateObjectLogger<SendLog>("sendlog");
  private final String _prefix;
  private Node node;
  private int protocolID;
  private long uploadBandwidth;
  private long bandwidthLeft;
  private long lastMessageSizeLeft;
  private int lastQueueSize = 0;
  private long queuedInfoSize = 0;
  Queue<Message> messageQueue = new LinkedList<Message>();
  int maxMessageStore = 50;
  LinkedList<String> sentChunkMessages = new LinkedList<String>();
  
  // private static Map<Long,Node> globalId2Node = new HashMap<Long, Node>();
  public PeersimNode(final String prefix) {
    super();
    final Double band = Common.currentConfiguration.uploadBandwidthDistribution
        .generateDistribution(Common.currentConfiguration.uploadBandwidthRandom);
    uploadBandwidth = band.isInfinite() ? Long.MAX_VALUE : band.longValue();
    bandwidthLeft = uploadBandwidth;
    lastMessageSizeLeft = 0;
    // globalId2Node.clear();
    _prefix = prefix;
  }
  
  /**
   * Peersim uses clone to create all nodes in the network.
   */
  @Override public Object clone() {
    return new PeersimNode(_prefix);
  }
  
  @Override public boolean isUp(final NodeAddress n) {
    return ((Fallible) n.node).isUp();
  }
  
  @Override public boolean sendMessage(final Message msg) {
    final long size = Utils.getSize(msg) + Common.currentConfiguration.messageHeaderSize;
    if (!isUp(msg.destID)) {
      System.err.println("WARN: sending message to offline node " + msg.destID);
      TextLogger.log(getAddress(), "WARN: sending message to offline node " + msg.destID + "\n");
    }
    if (msg instanceof ChunkMessage) {
      if (sentChunkMessages.contains(msg.toString())) {
        TextLogger.log(getAddress(), "WARN: sending duplicate chunk message\n");
        TextLogger.log(msg.destID, "WARN: sending duplicate chunk message\n");
        return false;
      }
    }
    if (size > bandwidthLeft) {
      TextLogger.log(getAddress(), "WARN: no more bandwidth\n");
      if (queuedInfoSize > uploadBandwidth && Common.currentConfiguration.waitQueueLimit) {
        TextLogger.log(getAddress(), "WARN: waitqueue limit exceeded - dropping message\n");
        TextLogger.log(msg.destID, "WARN: waitqueue limit exceeded - dropping message\n");
        return false;
      }
      if (bandwidthLeft != 0 && Common.currentConfiguration.sendPartialMessage) {
        lastMessageSizeLeft = size - bandwidthLeft;
        queuedInfoSize += lastMessageSizeLeft;
        bandwidthLeft = 0;
      } else {
        queuedInfoSize += size;
      }
      if (uploadBandwidth != Long.MAX_VALUE && queuedInfoSize >= (Common.currentConfiguration.playbackSeconds) * uploadBandwidth) {
        // message will never be sent
        TextLogger.log(getAddress(), "WARN: message will never be sent\n");
        TextLogger.log(msg.destID, "WARN: message will never be sent\n");
        return false;
      }
      messageQueue.add(msg);
      logChunkMessage(msg);
      return true;
    }
    bandwidthLeft -= size;
    internalSend(msg);
    logChunkMessage(msg);
    return true;
  }
  
  private void logChunkMessage(final Message msg) {
    if (msg instanceof ChunkMessage) {
      sentChunkMessages.add(msg.toString());
      if (sentChunkMessages.size() > maxMessageStore) {
        sentChunkMessages.removeFirst();
      }
    }
  }
  
  private void internalSend(final Message msg) {
    if (msg instanceof ChunkMessage) {
      sendLogger.logObject(new ChunkSendLog(msg.tag, msg.getClass().getSimpleName(), Utils.getSize(msg)
          + Common.currentConfiguration.messageHeaderSize, msg.sourceId.toString(), msg.destID.toString(), msg.isOverheadMessage(),
          ((ChunkMessage) msg).chunk.index));
    } else {
      sendLogger
      .logObject(new SendLog(msg.tag, msg.getClass().getSimpleName(), Utils.getSize(msg)
          + Common.currentConfiguration.messageHeaderSize, msg.sourceId.toString(), msg.destID.toString(), msg
          .isOverheadMessage()));
    }
    final Node peersimNode = (Node) nodeAddr.node;
    ((Transport) peersimNode.getProtocol(FastConfig.getTransport(protocolID))).send(peersimNode, (Node) msg.destID.node, msg,
        protocolID);
  }
  
  @Override public long getUploadBandwidth() {
    return uploadBandwidth;
  }
  
  @Override public long getEstimatedLatency(final NodeAddress key) {
    final int tranportID = FastConfig.getTransport(protocolID);
    final Transport transport = (Transport) node.getProtocol(tranportID);
    return transport.getLatency(node, (Node) key.node);
  }
  
  @Override public void processEvent(final Node n, final int pid, final Message event) {
    if (event instanceof SessionEndMessage) {
      TextLogger.log(getAddress(), "Ending session");
      NodeAdder.removeNode(n.getIndex());
      switch (Common.currentConfiguration.churnModel.type) {
        case sessionLengthAddOnFailure:
          NodeAdder.addNewNode();
          break;
        case sessionLengthOffLength:
          final Double delay = Common.currentConfiguration.churnModel.getOffLengthDistribution().generateDistribution(
              Common.currentConfiguration.churnModel.r) * 1000;
          NodeAdder.delayedAddNode(delay.longValue());
          break;
        case availabilityFile:
        case none:
        case sessionLengthInterArrival:
        case eventBased:
          break;
        default:
          throw new RuntimeException("no handling for case " + Common.currentConfiguration.churnModel.type);
      }
      return;
    }
    updateNodeIDState(n, pid);
    try {
      super.processEvent(event);
    } catch (final Throwable t) {
      System.err.println("node " + nodeAddr + " threw an exception!");
      throw (RuntimeException) t;
    }
  }
  
  @Override public void nextCycle(final Node n, final int pid) {
    if (!n.isUp()) {
      return;
    }
    updateNodeIDState(n, pid);
    uploadBandwidthUtilizationLog.logObject(new BandwidthLog(uploadBandwidth, uploadBandwidth - bandwidthLeft, queuedInfoSize,
        nodeAddr.toString()));
    bandwidthLeft = uploadBandwidth;
    sendDelayedMessages();
    if (messageQueue.size() != lastQueueSize) {
      TextLogger.log(nodeAddr, "message Queue size changed from " + lastQueueSize + " to " + messageQueue.size() + "\n");
      lastQueueSize = messageQueue.size();
    }
    try {
      client.nextCycle();
    } catch (final RuntimeException t) {
      System.err.println("node " + nodeAddr + " threw an exception!");
      throw t;
    } catch (final Throwable t) {
      System.err.println("node " + nodeAddr + " threw an exception!");
      throw new RuntimeException(t);
    }
  }
  
  private void sendDelayedMessages() {
    if (lastMessageSizeLeft > bandwidthLeft) {
      lastMessageSizeLeft -= bandwidthLeft;
      queuedInfoSize -= bandwidthLeft;
      bandwidthLeft = 0;
      return;
    }
    if (lastMessageSizeLeft > 0) {
      bandwidthLeft -= lastMessageSizeLeft;
      queuedInfoSize -= lastMessageSizeLeft;
      lastMessageSizeLeft = 0;
      internalSend(messageQueue.remove());
    }
    while (bandwidthLeft > 0 && !messageQueue.isEmpty()) {
      final Message msg = messageQueue.peek();
      final long size = Utils.getSize(msg) + Common.currentConfiguration.messageHeaderSize;
      if (bandwidthLeft < size) {
        if (Common.currentConfiguration.sendPartialMessage) {
          lastMessageSizeLeft = size - bandwidthLeft;
          queuedInfoSize -= bandwidthLeft;
          bandwidthLeft = 0;
        }
        break;
      }
      messageQueue.remove();
      internalSend(msg);
      bandwidthLeft -= size;
      queuedInfoSize -= size;
      if (queuedInfoSize < 0) {
        throw new IllegalStateException("queuedInfoSize lower than 0!");
      }
      continue;
    }
    if (messageQueue.isEmpty() && (queuedInfoSize != 0 || lastMessageSizeLeft != 0)) {
      throw new IllegalStateException("message queue is empty and queuedInfoSize=" + queuedInfoSize + ", lastMessageSizeLeft="
          + lastMessageSizeLeft);
    }
  }
  
  private NodeAvailability nodeAvail = null;
  
  void setNodeAvailability(final NodeAvailability nodeAvail) {
    this.nodeAvail = nodeAvail;
  }
  
  private void updateNodeIDState(final Node n, final int pid) {
    node = n;
    protocolID = pid;
    // globalId2Node.put(n.getID(), n);
    if (nodeAddr == null) {
      nodeAddr = new NodeAddress(n);
      if (!serverMode) {
        Double nextFail;
        switch (Common.currentConfiguration.churnModel.type) {
          case sessionLengthAddOnFailure:
          case sessionLengthOffLength:
          case sessionLengthInterArrival:
            nextFail = Common.currentConfiguration.churnModel.getSessionLengthDistribution().generateDistribution(
                Common.currentConfiguration.churnModel.r);
            break;
          case availabilityFile:
            nextFail = (double) ((nodeAvail.leaveTime - nodeAvail.joinTime) / 1000);
            break;
          case none:
          case eventBased:
            nextFail = Double.POSITIVE_INFINITY;
            break;
          default:
            throw new RuntimeException("no handling for case " + Common.currentConfiguration.churnModel.type);
        }
        if (!nextFail.isInfinite()) { // server never fails
          nextFail *= 1000;
          EDSimulator.add(nextFail.longValue(), new SessionEndMessage(nodeAddr), (Node) nodeAddr.node, pid);
        }
      }
      init();
      if (serverMode) {
        client.setServerMode();
        ((ServerVideoStream) client.player.getVs()).play();
      }
    } else {
      nodeAddr = new NodeAddress(n);
    }
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    uploadBandwidth = Common.currentConfiguration.serverUploadBandwidth;
    bandwidthLeft = uploadBandwidth;
  }
  
  @Override public void onKill() {
    if (nodeAddr != null) {
      churnLogger.logObject(new ChurnLog(nodeAddr.toString(), false, client.player.startupBuffering,
          client.player.bufferFromFirstChunk));
      TextLogger.log(nodeAddr, "left the system\n");
    }
    messageQueue.clear();
  }
  
  @Override public boolean blockingIsUp(final NodeAddress node1) {
    return isUp(node1);
  }
}
