package modules.network.peersim;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.edsim.NextCycleEvent;
import utils.Common;
import utils.Utils;
import entites.NodeAvailability;
import experiment.frameworks.NodeAddress;

public class ExperimentPlayer implements CDProtocol {
  @Override public Object clone() {
    return new ExperimentPlayer();
  }
  
  private static final String PAR_PROT = "protocol";
  public static int protocolID = 0;
  
  public ExperimentPlayer(final String prefix) {
    protocolID = Configuration.getPid(prefix + "." + PAR_PROT);
  }
  
  private ExperimentPlayer() {
    // no need to init the static field
  }
  
  public static void addNewNode(final NodeAvailability nodeAvail) {
    final Node newNode = (Node) Network.prototype.clone();
    ((PeersimNode) newNode.getProtocol(protocolID)).setServerNode(new NodeAddress(OneNodeInitialiser.serverNode));
    ((PeersimNode) newNode.getProtocol(protocolID)).setNodeAvailability(nodeAvail);
    Network.add(newNode);
    EDSimulator.add(0, new NextCycleEvent(null), newNode, protocolID);
  }
  
  @Override public void nextCycle(final Node node, final int pid) {
    if (!OneNodeInitialiser.serverNode.equals(node)) {
      return;
    }
    final Set<Integer> toAdd = new TreeSet<Integer>();
    final Map<Integer, NodeAvailability> nodeAvailability = Common.currentConfiguration.churnModel.getNodeAvailability();
    for (final Integer id : nodeAvailability.keySet()) {
      final NodeAvailability avail = nodeAvailability.get(id);
      if (avail.joinTime >= Utils.getTime()) {
        toAdd.add(id);
        addNewNode(avail);
      }
    }
    nodeAvailability.keySet().removeAll(toAdd);
  }
}