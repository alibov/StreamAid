package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import messages.ConnectionRequestMessage;
import messages.DisconnectMessage;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.swapLinks.OnlyInlinks;
import messages.swapLinks.OnlyOutlinks;
import messages.swapLinks.RandomWalk;
import messages.swapLinks.SwitchOutlinks;
import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public class SwapLinks extends OverlayModule<Integer[]> {
  // for each neighbor we count its inlinks and outlinks
  final int wl; // walk length
  final long capacity;
  
  public SwapLinks(final P2PClient client, final int wl, final int groupSize, final long _minCapacity,
      final long _minUploadBandwidth, final Random r) {
    super(client, r);
    this.wl = wl;
    final long minCapacity = _minCapacity;
    final long minUploadBandwidth = _minUploadBandwidth;
    capacity = ((long) ((double) network.getUploadBandwidth() / (double) minUploadBandwidth) * minCapacity);
    addIngredient(new RandomGroupBootstrapIngredient(groupSize, new Random(r.nextLong())), client);
  }
  
  private void addLink(final NodeAddress n, final boolean isOutLink) {
    final Integer[] l = neighborNodes.get(n);
    if (l == null) { //
      final Integer[] stats = { 0, 0 };
      neighborNodes.put(n, stats);
    }
    neighborNodes.get(n)[isOutLink ? 1 : 0]++;
  }
  
  private void addInlink(final NodeAddress n) {
    addLink(n, false);
  }
  
  private void addOutlink(final NodeAddress n) {
    addLink(n, true);
  }
  
  private void removeLink(final NodeAddress n, final boolean isOutLink) {
    final Integer[] l = neighborNodes.get(n);
    if (l != null) { // if there exists a link
      l[isOutLink ? 1 : 0]--;
      if (l[0] == 0 && l[1] == 0) { // if n is no longer a neighbor
        neighborNodes.remove(n);
      }
    }
  }
  
  private void removeOutlink(final NodeAddress n) {
    removeLink(n, true);
  }
  
  private void removeInlink(final NodeAddress n) {
    removeLink(n, false);
  }
  
  // only handles churn
  @Override public void nextCycle() {
    super.nextCycle();
    updateNeighbors();
    // printOutlinks();
  }
  
  /**
   * removes dead links and finds new ones
   */
  @Override protected void updateNeighbors() {
    final Set<NodeAddress> neighbors = neighborNodes.keySet();
    final Collection<NodeAddress> neighborsToReestablish = new LinkedList<NodeAddress>();
    for (final NodeAddress a : neighbors) {
      if (!neighbors.contains(a)) {
        continue;
      }
      if (network.isUp(a)) {
        continue;
      }
      neighborsToReestablish.add(a);
    }
    for (final NodeAddress a : neighborsToReestablish) {
      reestablishDeadLinks(a);
    }
  }
  
  private void printOutlinks() { // DEBUG
    System.out.print(network.getAddress().toString() + " to [");
    for (final NodeAddress n : neighborNodes.keySet()) {
      if (neighborNodes.get(n)[1] != 0) {
        System.out.print(n.toString() + " , ");
      }
    }
    System.out.print("] [");
    for (final NodeAddress n : neighborNodes.keySet()) {
      if (neighborNodes.get(n)[1] != 0) {
        System.out.print(neighborNodes.get(n)[1] + " , ");
      }
    }
    System.out.println("]");
  }
  
  /**
   * removes the neighbor a and replaces the links
   *
   * @param a
   */
  private void reestablishDeadLinks(final NodeAddress a) {
    if (!neighborNodes.containsKey(a)) {
      return;
    }
    final Integer[] links = neighborNodes.get(a);
    removeNeighbor(a);
    try { // works if the walk required to find new links is possible
      for (int i = 0; i < links[1]; i++) { // find new outlinks
        network.send(new OnlyInlinks(getMessageTag(), network.getAddress(), getRandomInlink(network.getAddress()), network
            .getAddress(), wl, OnlyInlinks.Algorithm.CHURN));
      }
      for (int i = 0; i < links[0]; i++) { // find new inlinks
        network.send(new OnlyOutlinks(getMessageTag(), network.getAddress(), getRandomOutlink(network.getAddress()), network
            .getAddress(), wl));
      }
    } catch (final NoSuchLink e) { // tell neighbors to disconnect me, and
      // then re-join
      for (final NodeAddress x : neighborNodes.keySet()) {
        network.send(new DisconnectMessage(getMessageTag(), network.getAddress(), x));
      }
      reConnect();
    }
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ConnectionRequestMessage) {
      if (((SwapLinksInfo) ((ConnectionRequestMessage<?>) message).payload).isOutLink) {
        addInlink(message.sourceId);
      } else {
        addOutlink(message.sourceId);
      }
      return;
    }
    // joins to the graph
    if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      final SeedNodeMultipleTargetsReplyMessage msg = ((SeedNodeMultipleTargetsReplyMessage) message);
      final List<NodeAddress> initialNodes = new ArrayList<NodeAddress>(msg.targets);
      for (int i = 0; i < capacity; i++) { // initiate onlyInlinks (join) an
        // amount of times according to the
        // capacity
        final NodeAddress target = initialNodes.get(r.nextInt(initialNodes.size())); // start
        // the
        // walk
        // from
        // random
        // nodes
        network.send(new OnlyInlinks(getMessageTag(), network.getAddress(), target, network.getAddress(), wl,
            OnlyInlinks.Algorithm.JOIN));
      }
      return;
    }
    if (message instanceof DisconnectMessage) {
      final DisconnectMessage msg = ((DisconnectMessage) message);
      reestablishDeadLinks(msg.sourceId);
      return;
    }
    if (message instanceof OnlyInlinks) {
      final OnlyInlinks msg = ((OnlyInlinks) message);
      if (msg.remainingSteps > 1) { // keep walking
        try {
          network.send(new OnlyInlinks(getMessageTag(), network.getAddress(), getRandomInlink(msg.origin), msg.origin,
              msg.remainingSteps - 1, msg.algo));
        } catch (final NoSuchLink e) { // stop the walk if there is no valid
          // step
          onlyInlinksFinalStep(msg);
        }
      } else { // finished walking
        onlyInlinksFinalStep(msg);
      }
      return;
    }
    // Deals with the OnlyOutlinks random walk
    if (message instanceof OnlyOutlinks) {
      final OnlyOutlinks msg = ((OnlyOutlinks) message);
      if (msg.remainingSteps > 1) {
        try {
          // we still have more steps to go in the random walk so we keep going
          network.send(new OnlyOutlinks(getMessageTag(), network.getAddress(), getRandomOutlink(msg.origin), msg.origin,
              msg.remainingSteps - 1));
        } catch (final NoSuchLink e) {
          // the current node has no outlinks so we end the walk here and make
          // the switch
          randomInlinkSwitchOutlinks(msg);
        }
      } else {
        // we finished the random walk successfully so we make the switch
        randomInlinkSwitchOutlinks(msg);
      }
      return;
    }
    // Instruct the node to switch outlinks from the source of the message to
    // some node
    if (message instanceof SwitchOutlinks) {
      final SwitchOutlinks msg = ((SwitchOutlinks) message);
      removeOutlink(msg.sourceId);
      addOutlink(msg.target);
      // notify the new outlink we have connected to it
      network.send(new ConnectionRequestMessage<SwapLinksInfo>(getMessageTag(), network.getAddress(), msg.target,
          new SwapLinksInfo(true)));
      return;
    }
  }
  
  // At the end of OnlyInlinks walk, we connect the origin to the end node
  private void onlyInlinksFinalStep(final OnlyInlinks msg) {
    addInlink(msg.origin);
    network.send(new ConnectionRequestMessage<SwapLinksInfo>(getMessageTag(), network.getAddress(), msg.origin, new SwapLinksInfo(
        false)));
    if (msg.algo == OnlyInlinks.Algorithm.JOIN) {
      // also if the walk was caused by JOIN algorithm
      // (and not because of CHURN, for example)
      // the origin steals an inlink from the end node
      randomInlinkSwitchOutlinks(msg);
    }
  }
  
  /**
   * Take a random inlink and make it switch outlinks from us to the origin of
   * the walk
   *
   * @param msg
   *          A message of a certain random walk. could be OnlyInlinks or
   *          OnlyOutlinks
   */
  private void randomInlinkSwitchOutlinks(final RandomWalk msg) {
    try {
      final NodeAddress last = getRandomInlink(msg.origin);
      removeInlink(last);
      network.send(new SwitchOutlinks(getMessageTag(), network.getAddress(), last, msg.origin));
    } catch (final NoSuchLink e) {
      addOutlink(msg.origin);
      network.send(new ConnectionRequestMessage<SwapLinksInfo>(getMessageTag(), network.getAddress(), msg.origin,
          new SwapLinksInfo(true)));
    }
  }
  
  class NoSuchLink extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
  }
  
  // gets a type of link and returns a random node connected to this node
  // by the specified link type, that is different from the origin
  // Note that the node is random with bias according to amount of connections
  // we have
  // to that node
  private NodeAddress getRandomLink(final boolean isOut, final NodeAddress origin) throws NoSuchLink {
    // take all the nodes that are connected to this node with the specified
    // link type
    final List<NodeAddress> subSet = filter(neighborNodes, new Boolean(isOut));
    while (subSet.remove(origin)) {
      // the loop is necessary because more than one copy of origin might be in
      // the list
    }
    if (subSet.isEmpty()) {
      // if no node qualified then we throw an exception
      throw new NoSuchLink();
    }
    // we return a random node from the qualified set
    return subSet.get(r.nextInt(subSet.size()));
  }
  
  private NodeAddress getRandomInlink(final NodeAddress origin) throws NoSuchLink {
    return getRandomLink(false, origin);
  }
  
  private NodeAddress getRandomOutlink(final NodeAddress origin) throws NoSuchLink {
    return getRandomLink(true, origin);
  }
  
  // return all inlinked nodes or all outlinked nodes
  private static List<NodeAddress> filter(final Map<NodeAddress, Integer[]> fullMap, final Boolean filter) {
    final List<NodeAddress> subSet = new ArrayList<NodeAddress>();
    for (final NodeAddress n : fullMap.keySet()) {
      final int nodeLinksNum = fullMap.get(n)[filter ? 1 : 0];
      for (int i = 0; i < nodeLinksNum; i++) {
        // we insert the node to the list a number of times
        // according to the amount of connections to that node
        subSet.add(n);
      }
    }
    return subSet;
  }
}

class SwapLinksInfo implements Sizeable {
  public final boolean isOutLink;
  
  public SwapLinksInfo(final boolean isOutLink) {
    super();
    this.isOutLink = isOutLink;
  }
  
  @Override public String toString() {
    return isOutLink ? "outlink" : "inlink";
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (isOutLink ? 1231 : 1237);
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
    final SwapLinksInfo other = (SwapLinksInfo) obj;
    return (isOutLink == other.isOutLink);
  }
  
  @Override public long getSimulatedSize() {
    return Integer.SIZE;
  }
}