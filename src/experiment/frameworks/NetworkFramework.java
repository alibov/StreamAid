package experiment.frameworks;

import java.net.InetSocketAddress;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import modules.network.ip.DatagramSocketNode;
import modules.network.ip.FailureDetector;
import utils.Common;
import utils.Utils;

public class NetworkFramework implements P2PFramework {
  public enum Role {
    server, client
  }
  
  private int port;
  private final Role role;
  private final InetSocketAddress serverAddress;
  private long endTime;
  private final FailureDetector fd;
  
  public NetworkFramework(final Role role, final int port, final InetSocketAddress serverAddress, final FailureDetector fd) {
    this.role = role;
    this.port = port;
    this.serverAddress = serverAddress;
    this.fd = fd;
  }
  
  @Override public void initFramework() {
    Utils.time = new Utils.Timeable() {
      @Override public long getTime() {
        return Calendar.getInstance(TimeZone.getTimeZone("EST"), Locale.US).getTime().getTime();
      }
    };
  }
  
  @Override public void commenceSingleRun(final int run) throws Exception {
    endTime = Utils.getTime() + new Long(Common.currentConfiguration.playbackSeconds) * 1000;
    if (Common.currentConfiguration.playbackSeconds == 0) {
      endTime = Long.MAX_VALUE;
    }
    switch (Common.currentConfiguration.churnModel.type) {
      case sessionLengthInterArrival:
      case availabilityFile:
      case eventBased:
        throw new UnsupportedOperationException(Common.currentConfiguration.churnModel.type
            + "is not supported for NetworkFramework");
      case none:
      case sessionLengthAddOnFailure:
      case sessionLengthOffLength:
      default:
    }
    final List<Thread> nodeList = new LinkedList<Thread>();
    for (int i = 0; i < Common.currentConfiguration.nodes; i++) {
      final DatagramSocketNode node = new DatagramSocketNode(port, endTime, fd.clone());
      port++;
      final Thread th = new Thread(node);
      nodeList.add(th);
      if (i == 0 && role.equals(Role.server)) {
        node.setServerMode();
        node.play();
        th.start();
        continue;
      }
      node.setServerNode(new NodeAddress(serverAddress, serverAddress.getHostName()));
      th.start();
    }
    final Set<Thread> toRemove = new HashSet<Thread>();
    final Map<Long, List<Thread>> toAdd = new TreeMap<Long, List<Thread>>();
    while (Utils.getTime() < endTime) {
      for (final Thread th : nodeList) {
        if (!th.isAlive()) {
          th.join();
          toRemove.add(th);
          switch (Common.currentConfiguration.churnModel.type) {
            case sessionLengthInterArrival:
            case availabilityFile:
            case eventBased:
              throw new UnsupportedOperationException(Common.currentConfiguration.churnModel.type
                  + "is not supported for NetworkFramework");
            case none:
              break;
            case sessionLengthAddOnFailure:
              final long now = Utils.getTime();
              Utils.checkExistence(toAdd, now, new LinkedList<Thread>());
              toAdd.get(now).add(new Thread(createNewClientNode()));
              break;
            case sessionLengthOffLength:
              final Double delay = Common.currentConfiguration.churnModel.getOffLengthDistribution().generateDistribution(
                  Common.currentConfiguration.churnModel.r) * 1000;
              final long time = Utils.getTime() + delay.longValue();
              Utils.checkExistence(toAdd, time, new LinkedList<Thread>());
              toAdd.get(time).add(new Thread(createNewClientNode()));
              break;
            default:
              throw new UnsupportedOperationException(Common.currentConfiguration.churnModel.type
                  + "is not supported for NetworkFramework");
          }
        }
      }
      nodeList.removeAll(toRemove);
      toRemove.clear();
      final Set<Long> added = new HashSet<Long>();
      for (final Entry<Long, List<Thread>> entry : toAdd.entrySet()) {
        if (entry.getKey() <= Utils.getTime()) {
          added.add(entry.getKey());
          for (final Thread th : entry.getValue()) {
            th.start();
            nodeList.add(th);
          }
          continue;
        }
        break;
      }
      toAdd.keySet().removeAll(added);
      Thread.sleep(1000);// TODO min between next departure and next arrival..
    }
    for (final Thread th : nodeList) {
      th.join();
    }
  }
  
  private DatagramSocketNode createNewClientNode() {
    final DatagramSocketNode node = new DatagramSocketNode(port, endTime, fd.clone());
    port++;
    node.setServerNode(new NodeAddress(serverAddress, serverAddress.getHostName()));
    return node;
  }
  
  @Override public boolean isSimulator() {
    return false;
  }
  
  @Override public int getMaxNodes() {
    return 10;
  }
  
  @Override public String toXml(final String prefix) {
    final StringBuilder sb = new StringBuilder();
    sb.append(prefix + "<framework value=\"" + getClass().getSimpleName() + "\" role=\"" + role.toString() + "\" port=\"" + port
        + "\" >");
    if (role.equals(Role.client)) {
      sb.append(prefix + "\t<address  hostname=\"" + serverAddress.getHostName() + "\" port=\"" + serverAddress.getPort() + "\" >");
    }
    sb.append(prefix + "</framework>");
    return sb.toString();
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (endTime ^ (endTime >>> 32));
    result = prime * result + port;
    result = prime * result + ((role == null) ? 0 : role.hashCode());
    result = prime * result + ((serverAddress == null) ? 0 : serverAddress.hashCode());
    return result;
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NetworkFramework other = (NetworkFramework) obj;
    if (endTime != other.endTime) {
      return false;
    }
    if (port != other.port) {
      return false;
    }
    if (role != other.role) {
      return false;
    }
    if (serverAddress == null) {
      if (other.serverAddress != null) {
        return false;
      }
    } else if (!serverAddress.equals(other.serverAddress)) {
      return false;
    }
    return true;
  }
}
