package modules.network.peersim;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import experiment.frameworks.NodeAddress;

/**
 * Initializes the advertiser nodes. Assigns target audience to each of them
 * 
 * @author Alexander Libov
 */
public class OneNodeInitialiser implements Control {
  // ------------------------------------------------------------------------
  // Constants
  // ------------------------------------------------------------------------
  private static final String PAR_PROT = "protocol";
  /** Protocol identifier; obtained from config property {@link #PAR_PROT}. */
  public static int pid = 0;
  public static Node serverNode;
  
  /**
   * Creates a new instance and read parameters from the config file.
   */
  public OneNodeInitialiser(final String prefix) {
    pid = Configuration.getPid(prefix + "." + PAR_PROT);
  }
  
  @Override public boolean execute() {
    final int adDistributor = CommonState.r.nextInt(Network.size());
    final PeersimNode prot = (PeersimNode) Network.get(adDistributor).getProtocol(pid);
    prot.setServerMode();
    System.err.println("server node is: " + adDistributor);
    serverNode = Network.get(adDistributor);
    for (int i = 0; i < Network.size(); i++) {
      final PeersimNode node = (PeersimNode) Network.get(i).getProtocol(pid);
      node.setServerNode(new NodeAddress(Network.get(adDistributor)));
    }
    return false;
  }
}
