package modules.network.peersim;

import java.util.Map.Entry;

import logging.TextLogger;
import logging.logObjects.ChurnLog;
import messages.Message;
import modules.network.NetworkModule;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.edsim.NextCycleEvent;
import utils.Common;
import experiment.ChurnModel;
import experiment.ChurnModel.Event;
import experiment.frameworks.NodeAddress;

public class NodeAdder implements CDProtocol, EDProtocol<Message> {
  @Override public Object clone() {
    return new NodeAdder();
  }
  
  private static final String PAR_PROT = "protocol";
  public static int protocolID = 0;
  public static int myPID = 0;
  
  public NodeAdder(final String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
  }
  
  private NodeAdder() {
    // no need to init the static field
  }
  
  @Override public void processEvent(final Node node, final int pid, final Message event) {
    if (event instanceof NodeAddMessage) {
      for (int i = 0; i < ((NodeAddMessage) event).amount; i++) {
        addNewNode();
      }
      if (!Common.currentConfiguration.churnModel.type.equals(ChurnModel.Type.sessionLengthInterArrival)) {
        return;
      }
      Double nextAddition = Common.currentConfiguration.churnModel.getInterArrivalDistribution().generateDistribution(
          Common.currentConfiguration.churnModel.r);
      nextAddition *= 1000;
      EDSimulator.add(nextAddition.longValue(), new NodeAddMessage(1), node, pid);
      return;
    }
    if (event instanceof NodeRemoveMessage) {
      for (int i = 0; i < ((NodeRemoveMessage) event).amount; i++) {
        removeRandomNode();
      }
      return;
    }
    throw new RuntimeException("illegal message received by NodeAdder: " + event);
  }
  
  public static void removeRandomNode() {
    int chosenNode = OneNodeInitialiser.serverNode.getIndex();
    while (Network.size() > 1 && chosenNode == OneNodeInitialiser.serverNode.getIndex()) {
      chosenNode = Common.currentConfiguration.churnModel.r.nextInt(Network.size());
    }
    removeNode(chosenNode);
  }
  
  public static void delayedAddNode(final long delay) {
    delayedAddNodes(delay, 1);
  }
  
  public static void delayedAddNodes(final long delay, final int amount) {
    EDSimulator.add(delay, new NodeAddMessage(amount), OneNodeInitialiser.serverNode, myPID);
  }
  
  public static void delayedRemoveNodes(final int delay, final int amount) {
    EDSimulator.add(delay, new NodeRemoveMessage(amount), OneNodeInitialiser.serverNode, myPID);
  }
  
  public static void addNewNode() {
    final Node newNode = (Node) Network.prototype.clone();
    ((PeersimNode) newNode.getProtocol(protocolID)).setServerNode(new NodeAddress(OneNodeInitialiser.serverNode));
    Network.add(newNode);
    EDSimulator.add(0, new NextCycleEvent(null), newNode, protocolID);
  }
  
  // this is executed exactly once
  @Override public void nextCycle(final Node node, final int pid) {
    myPID = pid;
    if (!OneNodeInitialiser.serverNode.equals(node)) {
      return;
    }
    switch (Common.currentConfiguration.churnModel.type) {
      case eventBased:
        for (final Entry<Integer, Event> i : Common.currentConfiguration.churnModel.eventMap.entrySet()) {
          switch (i.getValue().type) {
            case addition:
              delayedAddNodes(i.getKey() * 1000, i.getValue().amount);
              break;
            case departure:
              delayedRemoveNodes(i.getKey() * 1000, i.getValue().amount);
              break;
            default:
              throw new RuntimeException("unhandled event type " + i.getValue().type);
          }
        }
        break;
      case sessionLengthInterArrival:
        Double nextAddition = Common.currentConfiguration.churnModel.getInterArrivalDistribution().generateDistribution(
            Common.currentConfiguration.churnModel.r);
        if (nextAddition.isInfinite()) {
          return;
        }
        nextAddition *= 1000;
        EDSimulator.add(nextAddition.longValue(), new NodeAddMessage(1), node, pid);
        break;
      case sessionLengthOffLength:
      case availabilityFile:
      case none:
      case sessionLengthAddOnFailure:
      default:
        break;
    }
  }
  
  public static void removeNode(final int index) {
    final Node n = Network.remove(index);
    final NodeAddress impl = new NodeAddress(n);
    TextLogger.log(impl, "left the system\n");
    NetworkModule.churnLogger.logObject(new ChurnLog(impl.toString(), false, 0, false));
    Common.currentConfiguration.nodeRemoved(impl.toString());
  }
}