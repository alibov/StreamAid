package modules.network.ip;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

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
import utils.Common;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class DatagramSocketNode extends NetworkModule implements Runnable {
  protected static ObjectLogger<SendLog> sendLogger = ObjectLogger.getLogger("sendlog");
  FailureDetector FD = null;
  final Object queueLock = new Object();
  final Queue<Message> messageQueue = new LinkedList<Message>();
  private final long uploadBandwidth;
  DatagramSocket socket;
  // private final long pingWaitThreshold =
  // Common.currentConfiguration.cycleLength / 5;
  private long sentDataSize = 0;
  private long bitsInQueue = 0;
  private final long streamEndTime;
  
  public DatagramSocketNode(final int port, final long streamEndTime, final FailureDetector fd) {
    FD = fd;
    FD.setNode(this);
    uploadBandwidth = (long) Common.currentConfiguration.uploadBandwidthDistribution
        .generateDistribution(Common.currentConfiguration.uploadBandwidthRandom);
    this.streamEndTime = streamEndTime;
    try {
      socket = new DatagramSocket(port);
      final InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), socket.getLocalPort());
      nodeAddr = new NodeAddress(inetSocketAddress, Utils.getLocalHostName());
      super.init();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override public long getEstimatedLatency(final NodeAddress key) {
    // TODO implement
    return 0;
  }
  
  @Override public long getUploadBandwidth() {
    return uploadBandwidth;
  }
  
  public void play() {
    ((ServerVideoStream) client.player.getVs()).play();
  }
  
  @Override public void run() {
    System.out.println("node " + nodeAddr + " is up!");
    long endTime;
    switch (Common.currentConfiguration.churnModel.type) {
      case sessionLengthInterArrival:
      case availabilityFile:
      case eventBased:
        throw new UnsupportedOperationException(Common.currentConfiguration.churnModel.type
            + "is not supported for NetworkFramework");
      case none:
        endTime = streamEndTime;
        break;
      case sessionLengthAddOnFailure:
      case sessionLengthOffLength:
        if (!serverMode) {
          endTime = Math.min(
              new Date().getTime()
              + new Double(Common.currentConfiguration.churnModel.getSessionLengthDistribution().generateDistribution(
                  Common.currentConfiguration.churnModel.r) * 1000).longValue(), streamEndTime);
        } else {
          endTime = streamEndTime;
        }
        break;
      default:
        throw new UnsupportedOperationException(Common.currentConfiguration.churnModel.type
            + "is not supported for NetworkFramework");
    }
    final long constEndTime = endTime;
    final Thread messageThread = new Thread(new Runnable() {
      @Override public void run() {
        final int bufferLength = Common.currentConfiguration.bitRate / 8 * 120/* max
         * chunks
         * per
         * message */;
        final byte[] receiveData = new byte[bufferLength];
        final DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        while (new Date().getTime() < constEndTime) {
          try {
            socket.setSoTimeout(Math.max((int) (constEndTime - new Date().getTime()), 1));
            socket.receive(receivePacket);
          } catch (final SocketTimeoutException e) {
            continue;
          } catch (final Throwable e) {
            e.printStackTrace();
            continue;
          }
          try {
            final Object obj = Utils.deserializeObject(receivePacket.getData());
            final Message m = (Message) obj;
            final NodeAddress realSource = new NodeAddress(new InetSocketAddress(receivePacket.getAddress(),
                receivePacket.getPort()), m.sourceId.getName());
            FD.receivedMessage(realSource);
            if (obj instanceof PingMessage) {
              TextLogger.log(getAddress(), "got ping from " + realSource + ". sending pong\n");
              final byte[] message = Utils.serializeObject(new PongMessage(getAddress()));
              DatagramPacket packet;
              try {
                packet = new DatagramPacket(message, message.length, new InetSocketAddress(receivePacket.getAddress(),
                    receivePacket.getPort()));
                socket.send(packet);
              } catch (final IOException e) {
                e.printStackTrace();
              }
              continue;
            }
            if (obj instanceof PongMessage) {
              TextLogger.log(getAddress(), "got pong from " + realSource + "\n");
              continue;
            }
            m.updateSourceID(realSource);
            synchronized (queueLock) {
              messageQueue.add(m);
              queueLock.notify();
            }
          } catch (final Throwable t) {
            t.printStackTrace();
          }
        }
      }
    });
    messageThread.start();
    while (new Date().getTime() < endTime) {
      try {
        final long time = new Date().getTime();
        client.nextCycle();
        // one cycle every second
        while (new Date().getTime() < time + Common.currentConfiguration.cycleLength) {
          Message event = null;
          synchronized (queueLock) {
            if (messageQueue.isEmpty()) {
              queueLock.wait(Math.max(time + Common.currentConfiguration.cycleLength - new Date().getTime(), 1));
            } else {
              event = messageQueue.remove();
            }
          }
          if (event != null) {
            processEvent(event);
          }
        }
        bitsInQueue += sentDataSize - uploadBandwidth;
        bitsInQueue = bitsInQueue < 0 ? 0 : bitsInQueue;
        uploadBandwidthUtilizationLog.logObject(new BandwidthLog(uploadBandwidth, Math.min(sentDataSize, uploadBandwidth),
            bitsInQueue, nodeAddr.toString()));
        sentDataSize = 0;
      } catch (final RuntimeException t) {
        System.err.println("node " + nodeAddr + " threw an exception!");
        throw t;
      } catch (final Throwable t) {
        System.err.println("node " + nodeAddr + " threw an exception!");
        throw new RuntimeException(t);
      }
    }
    System.out.println("node " + nodeAddr + " finished cycles");
    try {
      messageThread.join();
    } catch (final Throwable t) {
      System.err.println("node " + nodeAddr + " threw an exception!");
      throw (RuntimeException) t;
    }
    System.out.println("node " + nodeAddr + " finished handling messages");
    churnLogger.logObject(new ChurnLog(nodeAddr.toString(), false, client.player.startupBuffering,
        client.player.bufferFromFirstChunk));
    TextLogger.log(nodeAddr, "left the system\n");
    if (client.player.getVs() != null && !serverMode) {
      System.out.println("CI: " + client.player.getCI());
      System.out.println("Latency: " + client.player.getLatency());
      System.out.println("Vital Chunks Received: " + client.network.getVitalChunksReceived());
      System.out.println("Duplicate Chunks Received: " + client.network.getDuplicateChunksReceived());
      System.out.println("Duplicate/Vital:" + ((double) client.network.getDuplicateChunksReceived())
          / client.network.getVitalChunksReceived());
    }
    socket.close();
  }
  
  @Override public boolean sendMessage(final Message msg) {
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
    try {
      final InetSocketAddress destAddr = (InetSocketAddress) msg.destID.node;
      final byte[] message = Utils.serializeObject(msg);
      sentDataSize += message.length;
      // Initialize a datagram packet with data and address
      final DatagramPacket packet = new DatagramPacket(message, message.length, destAddr);
      socket.send(packet);
      return true;
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    client.setServerMode();
  }
  
  void setNodeImpl(final NodeAddress nodeImpl) {
    nodeAddr = nodeImpl;
  }
  
  @Override public boolean blockingIsUp(final NodeAddress node) {
    return FD.blockingIsUp(node);
  }
  
  @Override public boolean isUp(final NodeAddress node) {
    return FD.isUp(node);
  }
}
