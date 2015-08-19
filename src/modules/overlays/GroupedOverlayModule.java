package modules.overlays;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import logging.TextLogger;
import modules.P2PClient;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public abstract class GroupedOverlayModule<T> extends OverlayModule<T> {
  public final static String fathersGroupName = "FATHERS";
  public final static String sonsGroupName = "SONS";
  private final Map<String, Set<NodeAddress>> nodeGroup = new HashMap<String, Set<NodeAddress>>();
  private final Map<NodeAddress, Set<String>> nodeToGroups = new HashMap<NodeAddress, Set<String>>();
  
  public GroupedOverlayModule(final P2PClient client, final Random r) {
    super(client, r);
    nodeGroup.put(fathersGroupName, new TreeSet<NodeAddress>());
    nodeGroup.put(sonsGroupName, new TreeSet<NodeAddress>());
  }
  
  public Set<NodeAddress> getNodeGroup(final String group) {
    return getNodeGroup(group, true);
  }
  
  public Set<NodeAddress> getNodeGroup(final String group, final boolean checkLiveness) {
    Utils.checkExistence(nodeGroup, group, new TreeSet<NodeAddress>());
    final Set<NodeAddress> retVal = nodeGroup.get(group);
    if (!checkLiveness) {
      return retVal;
    }
    final Collection<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : retVal) {
      if (!network.isUp(n)) {
        toRemove.add(n);
      }
    }
    retVal.removeAll(toRemove);
    removeNeighbors(toRemove);
    return retVal;
  }
  
  public void addToGroup(final String group, final NodeAddress nodeToAdd) {
    Utils.checkExistence(nodeGroup, group, new TreeSet<NodeAddress>());
    Utils.checkExistence(nodeToGroups, nodeToAdd, new TreeSet<String>());
    nodeToGroups.get(nodeToAdd).add(group);
    addNeighbor(nodeToAdd);
    nodeGroup.get(group).add(nodeToAdd);
    TextLogger.log(network.getAddress(), "adding neighbor: " + nodeToAdd + " to group " + group + " via "
        + this.getClass().getSimpleName() + "\n");
  }
  
  public void addToGroup(final String group, final Collection<NodeAddress> nodes) {
    for (final NodeAddress n : nodes) {
      addNeighbor(n);
      Utils.checkExistence(nodeToGroups, n, new TreeSet<String>());
      nodeToGroups.get(n).add(group);
      TextLogger.log(network.getAddress(), "adding neighbor: " + n + " to group " + group + " via "
          + this.getClass().getSimpleName() + "\n");
    }
    Utils.checkExistence(nodeGroup, group, new TreeSet<NodeAddress>());
    nodeGroup.get(group).addAll(nodes);
  }
  
  public boolean removeFromGroup(final NodeAddress n, final String group) {
    if (!nodeToGroups.containsKey(n)) {
      return false;
    }
    TextLogger.log(network.getAddress(), "removing neighbor: " + n + " from group " + group + " via "
        + this.getClass().getSimpleName() + "\n");
    nodeToGroups.get(n).remove(group);
    if (nodeToGroups.get(n).isEmpty()) {
      nodeToGroups.remove(n);
      super.removeNeighbor(n);
    }
    return nodeGroup.get(group).remove(n);
  }
  
  @Override public boolean removeNeighbors(final Collection<NodeAddress> toRemove) {
    for (final NodeAddress n : toRemove) {
      if (!nodeToGroups.containsKey(n)) {
        continue;
      }
      for (final String group : nodeToGroups.get(n)) {
        nodeGroup.get(group).remove(n);
        TextLogger.log(network.getAddress(), "removing neighbor: " + n + " from group " + group + " via "
            + this.getClass().getSimpleName() + "\n");
      }
      nodeToGroups.remove(n);
    }
    return super.removeNeighbors(toRemove);
  }
  
  @Override public boolean removeNeighbor(final NodeAddress toRemove) {
    if (nodeToGroups.containsKey(toRemove)) {
      for (final String group : nodeToGroups.get(toRemove)) {
        nodeGroup.get(group).remove(toRemove);
        TextLogger.log(network.getAddress(), "removing neighbor: " + toRemove + " from group " + group + " via "
            + this.getClass().getSimpleName() + "\n");
      }
      nodeToGroups.remove(toRemove);
    }
    return super.removeNeighbor(toRemove);
  }
  
  @Override public boolean isOverlayConnected() {
    if (!network.isServerMode()) {
      return !getNodeGroup(fathersGroupName).isEmpty();
    }
    return super.isOverlayConnected();
  }
}
