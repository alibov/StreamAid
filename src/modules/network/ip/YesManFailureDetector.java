package modules.network.ip;

import experiment.frameworks.NodeAddress;

public class YesManFailureDetector extends FailureDetector {
  @Override public boolean isUp(final NodeAddress node) {
    return true;
  }
  
  @Override public boolean blockingIsUp(final NodeAddress node) {
    return true;
  }
  
  @Override public void receivedMessage(final NodeAddress source) {
//do nothing
  }
  
  @Override public FailureDetector clone() {
    return new YesManFailureDetector();
  }
}
