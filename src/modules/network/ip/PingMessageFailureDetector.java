package modules.network.ip;

import experiment.frameworks.NodeAddress;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import logging.TextLogger;

public class PingMessageFailureDetector extends FailureDetector {
  Map<NodeAddress, Long/* last checked */> neighborsLastPonged = new ConcurrentHashMap<NodeAddress, Long>();
  Map<NodeAddress, Long/* last checked */> neighborsLastPinged = new ConcurrentHashMap<NodeAddress, Long>();
  private final long pingThreshold = 500;
  private final long minTimeBetweenPings = 250;
  private final long pongThreshold = 2000;
  
  @Override public FailureDetector clone() {
    return new PingMessageFailureDetector();
  }
  
  @Override public boolean isUp(final NodeAddress node) {
    if (nNode.getAddress().equals(node)) {
      return true;
    }
    if (!neighborsLastPonged.containsKey(node)) {
      neighborsLastPonged.put(node, Long.MIN_VALUE);
    }
    if (new Date().getTime() - neighborsLastPonged.get(node) < pongThreshold) {
      return true;
    }
    if (!neighborsLastPinged.containsKey(node)) {
      TextLogger.log(nNode.getAddress(), "no ping, sending ping to " + node + "\n");
      pingNode(node);
      return true;
    }
    if (neighborsLastPinged.get(node) <= neighborsLastPonged.get(node)) {
      TextLogger.log(nNode.getAddress(), "no ping after last pong, sending ping to " + node + "\n");
      pingNode(node);
      return true;
    }
    if (new Date().getTime() - neighborsLastPinged.get(node) < pingThreshold) {
      if (new Date().getTime() - neighborsLastPinged.get(node) > minTimeBetweenPings) {
        TextLogger.log(nNode.getAddress(), "no ping after last ping, sending ping to " + node + "\n");
        pingNode(node);
      }
      return true;
    }
    return false;
  }
  
  private void pingNode(final NodeAddress node) {
    neighborsLastPinged.put(node, new Date().getTime());
    nNode.send(new PingMessage(nNode.getAddress(), node));
  }
  
  @Override public boolean blockingIsUp(final NodeAddress node) {
    // TODO implement
    return false;
  }
  
  @Override public void receivedMessage(final NodeAddress source) {
    neighborsLastPonged.put(source, new Date().getTime());
  }
}
