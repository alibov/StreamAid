package modules.overlays;

import ingredients.overlay.DoubleRemoveIngredient;
import ingredients.overlay.InformationExchange;
import interfaces.NodeConnectionAlgorithm;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.ObjectLogger;
import logging.TextLogger;
import logging.logObjects.DegreeLog;
import modules.P2PClient;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public abstract class OverlayModule<T> extends NodeConnectionAlgorithm {
  protected final TreeMap<NodeAddress, T> neighborNodes = new TreeMap<NodeAddress, T>();
  protected final TreeSet<NodeAddress> knownNodes = new TreeSet<NodeAddress>();
  protected static ObjectLogger<DegreeLog> degreeLogger = ObjectLogger.getLogger("degreeLog");
  protected static ObjectLogger<DegreeLog> secondDegreeLogger = ObjectLogger.getLogger("secDegreeLog");
  private long lastDegreeLogTime;
  private int lastDegree;
  public final InformationExchange infoExchange;
  public final DoubleRemoveIngredient DRB;
  
  public OverlayModule(final P2PClient client, final Random r) {
    super(client.network, r);
    lastDegreeLogTime = Utils.getTime();
    lastDegree = 0;
    addIngredient(infoExchange = new InformationExchange(), client);
    addIngredient(DRB = new DoubleRemoveIngredient(), client);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    DRB.removeNeighbors(getNeighbors());
  }
  
  public void addNeighbor(final NodeAddress neighbor, final T info) {
    if (neighbor.equals(network.getAddress())) {
      throw new RuntimeException(network.getAddress() + " is trying to connect to itself");
    }
    knownNodes.add(neighbor);
    neighborNodes.put(neighbor, info);
    TextLogger.log(network.getAddress(), "is now connected to " + neighbor + " via " + this.getClass().getSimpleName() + "\n");
    TextLogger.log(network.getAddress(), "neighbors are " + getNeighbors() + " via " + this.getClass().getSimpleName() + "\n");
    logDegree();
  }
  
  public void addNeighbor(final NodeAddress neighbor) {
    addNeighbor(neighbor, null);
  }
  
  public boolean isOverlayConnected() {
    updateNeighbors();
    return !neighborNodes.isEmpty();
  }
  
  public Set<NodeAddress> getNeighbors() {
    updateNeighbors();
    return neighborNodes.keySet();
  }
  
  protected void updateNeighbors() {
    final Collection<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress neighbor : neighborNodes.keySet()) {
      if (!network.isUp(neighbor)) {
        toRemove.add(neighbor);
        knownNodes.remove(neighbor);
      }
    }
    if (!toRemove.isEmpty()) {
      removeNeighbors(toRemove);
    }
  }
  
  protected void updateKnown() {
    final Collection<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress neighbor : knownNodes) {
      if (!network.isUp(neighbor)) {
        toRemove.add(neighbor);
      }
    }
    if (!toRemove.isEmpty()) {
      knownNodes.removeAll(toRemove);
    }
  }
  
  public void updateKnownNodes() {
    final Collection<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : knownNodes) {
      if (!network.isUp(n)) {
        toRemove.add(n);
      }
    }
    if (!toRemove.isEmpty()) {
      knownNodes.removeAll(toRemove);
    }
  }
  
  public boolean removeNeighbors(final Collection<NodeAddress> toRemove) {
    final boolean retVal = neighborNodes.keySet().removeAll(toRemove);
    if (!toRemove.isEmpty()) {
      TextLogger.log(network.getAddress(), "removing failed neighbors: " + toRemove + " via " + this.getClass().getSimpleName()
          + "\n");
      TextLogger.log(network.getAddress(), "neighbors are " + getNeighbors() + " via " + this.getClass().getSimpleName() + "\n");
      logDegree();
    }
    return retVal;
  }
  
  /**
   *
   * @param toRemove
   * @return true if the neighbor was removed
   */
  public boolean removeNeighbor(final NodeAddress toRemove) {
    final boolean retVal = neighborNodes.remove(toRemove) != null;
    if (retVal) {
      TextLogger.log(network.getAddress(), "removing neighbor: " + toRemove + " via " + this.getClass().getSimpleName() + "\n");
      TextLogger.log(network.getAddress(), "neighbors are " + getNeighbors() + " via " + this.getClass().getSimpleName() + "\n");
      logDegree();
    }
    return retVal;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    secondDegreeLogger.logObject(new DegreeLog(network.getAddress().toString(), getMessageTag(), neighborNodes.size(), secondsUp));
  }
  
  public void reConnect() {
    TextLogger.log(network.getAddress(), "reconnecting\n");
    final Collection<NodeAddress> toRemove = new TreeSet<NodeAddress>(neighborNodes.keySet());
    removeNeighbors(toRemove);
  }
  
  private void logDegree() {
    if (lastDegree == neighborNodes.size()) {
      return;
    }
    degreeLogger.logObject(new DegreeLog(network.getAddress().toString(), getMessageTag(), neighborNodes.size(), Utils.getTime()
        - lastDegreeLogTime));
    lastDegreeLogTime = Utils.getTime();
    lastDegree = neighborNodes.size();
  }
  
  protected T getNeighborInfo(final NodeAddress neighbor) {
    return neighborNodes.get(neighbor);
  }
  
  protected void setNeighborInfo(final NodeAddress neighbor, final T info) {
    if (!neighborNodes.containsKey(neighbor)) {
      addNeighbor(neighbor, info);
      return;
    }
    neighborNodes.put(neighbor, info);
  }
  
  final TreeMap<NodeAddress, T> getNeigborTable() {
    updateNeighbors();
    return neighborNodes;
  }
  
  public final TreeSet<NodeAddress> getKnownNodes() {
    updateKnown();
    return knownNodes;
  }
}
