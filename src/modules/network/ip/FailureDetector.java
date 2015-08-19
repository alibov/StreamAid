package modules.network.ip;

import modules.network.FailureDetectorInterface;
import modules.network.NetworkModule;
import experiment.frameworks.NodeAddress;

public abstract class FailureDetector implements FailureDetectorInterface {
  protected NetworkModule nNode = null;
  
  public void setNode(final NetworkModule node) {
    if (nNode != null) {
      throw new RuntimeException("node in FD can be set only once!");
    }
    nNode = node;
  }
  
  public abstract void receivedMessage(NodeAddress source);
  
  @Override public abstract FailureDetector clone();
}
