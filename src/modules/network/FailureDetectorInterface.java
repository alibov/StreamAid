package modules.network;

import experiment.frameworks.NodeAddress;

public interface FailureDetectorInterface {
  public boolean isUp(NodeAddress node);
  
  public boolean blockingIsUp(NodeAddress node);
}
