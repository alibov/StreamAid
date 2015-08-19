package utils.chunkySpread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import experiment.frameworks.NodeAddress;

public class PotentialParentsQueue {
  private final ArrayList<Boolean> waitingForParentReply = new ArrayList<Boolean>();
  private final ArrayList<Queue<NodeAddress>> potentialParents = new ArrayList<Queue<NodeAddress>>();
  
  public boolean isWaitingForParentReply(final int treeID) {
    if (null == waitingForParentReply.get(treeID)) {
      waitingForParentReply.add(treeID, false);
    }
    return waitingForParentReply.get(treeID);
  }
  
  public void insertParent(final int treeID, final NodeAddress parent) {
    if (null == potentialParents.get(treeID)) {
      potentialParents.add(treeID, new LinkedList<NodeAddress>());
    }
    potentialParents.get(treeID).add(parent);
  }
  
  public boolean isTreeEmpty(final int treeID) {
    return (null == potentialParents.get(treeID)) || (potentialParents.get(treeID).isEmpty());
  }
  
  public NodeAddress removeParent(final int treeID) {
    return potentialParents.get(treeID).remove();
  }
  
  public void setWaitingForParentReply(final int treeID, final boolean waitingForParentReply) {
    this.waitingForParentReply.add(treeID, waitingForParentReply);
  }
  
  public void flush(final int treeID) {
    potentialParents.add(treeID, new LinkedList<NodeAddress>());
    waitingForParentReply.add(treeID, false);
  }
}
