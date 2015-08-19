package modules.overlays;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import modules.P2PClient;
import utils.Common;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public abstract class MultipleTreeOverlayModule<T> extends GroupedOverlayModule<T> {
  public MultipleTreeOverlayModule(final P2PClient client, final Random r) {
    super(client, r);
  }
  
  /* map represents the tree - descriptor to sons */
  public Map<Integer, Set<NodeAddress>> descriptorToSons = new TreeMap<Integer, Set<NodeAddress>>();
  
  public Set<NodeAddress> getSonNodes(final Integer descriptor) {
    return getSonNodes(descriptor, true);
  }
  
  public Set<NodeAddress> getSonNodes(final Integer descriptor, final boolean removeFailed) {
    Utils.checkExistence(descriptorToSons, descriptor, new TreeSet<NodeAddress>());
    if (removeFailed) {
      final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
      for (final NodeAddress n : descriptorToSons.get(descriptor)) {
        if (!network.isUp(n)) {
          toRemove.add(n);
        }
      }
      descriptorToSons.get(descriptor).removeAll(toRemove);
    }
    return descriptorToSons.get(descriptor);
  }
  
  public String getDownloadGroupName(final int club) {
    return club + "down";
  }
  
  public String getUploadGroupName(final int club) {
    return club + "up";
  }
  
  /* @Override public boolean isOverlayConnected() { for (int i = 0; i <
   * Common.currentConfiguration.descriptions; ++i) { if
   * (getNodeGroup(getDownloadGroupName(i)).isEmpty()) { return false; } }
   * return true; } */
  @Override public boolean isOverlayConnected() {
    for (int i = 0; i < Common.currentConfiguration.descriptions; ++i) {
      if (!getNodeGroup(getDownloadGroupName(i)).isEmpty()) {
        return true;
      }
    }
    return false;
  }
}
