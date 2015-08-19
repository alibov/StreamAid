package modules.network.ip;

import experiment.frameworks.NodeAddress;

public class PingFailureDetector extends FailureDetector {
  @Override public boolean isUp(final NodeAddress node) {
    return blockingIsUp(node);
  }
  
  @Override public boolean blockingIsUp(final NodeAddress node) {
    try {
      String cmd = "";
      if (System.getProperty("os.name").startsWith("Windows")) {
        // For Windows
        cmd = "ping -n 1 " + node.getName();
      } else {
        // For Linux and OSX
        cmd = "ping -c 1 " + node.getName();
      }
      final Process myProcess = Runtime.getRuntime().exec(cmd);
      myProcess.waitFor();
      return (myProcess.exitValue() == 0);
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  
  @Override public void receivedMessage(final NodeAddress source) {
    // TODO Auto-generated method stub
  }
  
  @Override public FailureDetector clone() {
    return new PingFailureDetector();
  }
}
