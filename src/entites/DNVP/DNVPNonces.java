package entites.DNVP;

import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.DNVP.NewNoncesMessage;
import messages.DNVP.NoncesDelivered;
import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public class DNVPNonces {
  private final P2PClient client;
  private final int numOfNoncesToProduce;
  private final String messageTag;
  public final Map<NodeAddress, LinkedList<Nonce>> neighborsNeighborToNonces = new TreeMap<NodeAddress, LinkedList<Nonce>>();
  private final Map<NodeAddress, Long> neighborsNeighborToRoundIssuedNonces = new TreeMap<NodeAddress, Long>();
  private Random r;
  
  public DNVPNonces(final P2PClient client, final int numOfNoncesToProduce, final String messageTag, final Random r) {
    this.r = r;
    this.client = client;
    this.numOfNoncesToProduce = numOfNoncesToProduce;
    this.messageTag = messageTag;
  }
  
  public DNVPNonces(final DNVPNonces dnvpNonces) {
    for (final Map.Entry<NodeAddress, LinkedList<Nonce>> entry : dnvpNonces.neighborsNeighborToNonces.entrySet()) {
      final LinkedList<Nonce> listCopy = new LinkedList<Nonce>();
      for (final Nonce l : entry.getValue()) {
        listCopy.add(l);
      }
      neighborsNeighborToNonces.put(entry.getKey(), listCopy);
    }
    for (final Map.Entry<NodeAddress, Long> entry : dnvpNonces.neighborsNeighborToRoundIssuedNonces.entrySet()) {
      neighborsNeighborToRoundIssuedNonces.put(entry.getKey(), entry.getValue());
    }
    client = dnvpNonces.client;
    numOfNoncesToProduce = dnvpNonces.numOfNoncesToProduce;
    messageTag = dnvpNonces.messageTag;
  }
  
  public void addNoncesForNeighbor(final NodeAddress neighborsNeighbor, final LinkedList<Nonce> nonces) {
    if (nonces.size() <= 0) {
      return;
    }
    if (neighborsNeighborToNonces.get(neighborsNeighbor) == null) {
      neighborsNeighborToNonces.put(neighborsNeighbor, new LinkedList<Nonce>());
    }
    neighborsNeighborToNonces.get(neighborsNeighbor).addAll(nonces);
  }
  
  public void addNonceForNeighbor(final NodeAddress neighborsNeighbor, final Nonce nonce) {
    if (neighborsNeighborToNonces.get(neighborsNeighbor) == null) {
      neighborsNeighborToNonces.put(neighborsNeighbor, new LinkedList<Nonce>());
    }
    neighborsNeighborToNonces.get(neighborsNeighbor).add(nonce);
  }
  
  public static LinkedList<Nonce> generateNonces(final int numOfNoncesToProduce, final Random r) {
    final LinkedList<Nonce> nonces = new LinkedList<Nonce>();
    for (int i = 0; i < numOfNoncesToProduce; i++) {
      nonces.add(Nonce.generateNonce(r));
    }
    return nonces;
  }
  
  public Set<NodeAddress> getNeighbors() {
    return neighborsNeighborToNonces.keySet();
  }
  
  public void removeNeighborAndItsNonces(final NodeAddress neighborsNeighbor) {
    neighborsNeighborToNonces.remove(neighborsNeighbor);
    neighborsNeighborToRoundIssuedNonces.remove(neighborsNeighbor);
  }
  
  public void removeNeighborsAndItsNonces(final Set<NodeAddress> neighborsNeighbors) {
    for (final NodeAddress neighborsNeighbor : neighborsNeighbors) {
      removeNeighborAndItsNonces(neighborsNeighbor);
    }
  }
  
  public NodeAddress checkVerificationMessageAndUpdate(final Map<NodeAddress, LinkedList<Nonce>> verficationNonces,
      final Set<NodeAddress> neighborsToCheck, final Long lastRoundVerified, final Long currentRound) {
    // Removing neighbors that this node generated nonces for them, but didn't
    // receive from them any nonce in this verification. Return these nodes to
    // caller so it will check with them if there was a disconnection.
    final Set<NodeAddress> neighborsWhoDidntSendNonces = new TreeSet<NodeAddress>();
    neighborsWhoDidntSendNonces.addAll(neighborsNeighborToNonces.keySet());
    neighborsWhoDidntSendNonces.removeAll(verficationNonces.keySet());
    for (final NodeAddress node : neighborsWhoDidntSendNonces) {
      neighborsNeighborToNonces.remove(node);
      neighborsNeighborToRoundIssuedNonces.remove(node);
      neighborsToCheck.add(node);
    }
    // Check The nonces I did receive and update my expected nonces accordingly.
    final Set<NodeAddress> neighborsToRemove = new TreeSet<NodeAddress>();
    for (final Map.Entry<NodeAddress, LinkedList<Nonce>> neighborNeighborToReceivedNonces : verficationNonces.entrySet()) {
      final LinkedList<Nonce> noncesExpectedFromNeighborsNeighbor = neighborsNeighborToNonces.get(neighborNeighborToReceivedNonces
          .getKey());
      if (noncesExpectedFromNeighborsNeighbor == null) {
        TextLogger.log(client.network.getAddress(), "No nonces expected from " + neighborNeighborToReceivedNonces.getKey()
            + " via " + this.getClass().getSimpleName() + "\n");
        neighborsToRemove.add(neighborNeighborToReceivedNonces.getKey());
      } else {
        if (neighborNeighborToReceivedNonces.getValue().size() + 1 + lastRoundVerified < currentRound) {
          // throw new IllegalStateException("Not enough nonces from " +
          // neighborNeighborToReceivedNonces.getKey());
          return neighborNeighborToReceivedNonces.getKey();
        }
        for (final Nonce nonce : neighborNeighborToReceivedNonces.getValue()) {
          while (!noncesExpectedFromNeighborsNeighbor.isEmpty() && !nonce.equals(noncesExpectedFromNeighborsNeighbor.getFirst())) {
            noncesExpectedFromNeighborsNeighbor.removeFirst();
          }
          if (noncesExpectedFromNeighborsNeighbor.isEmpty()) {
            throw new IllegalStateException("Nonces do not match "); // + nonce
            // + " ; "
            // +
            // noncesExpectedFromNeighborsNeighbor.getFirst());
            // return neighborNeighborToReceivedNonces.getKey();
          }
          noncesExpectedFromNeighborsNeighbor.removeFirst();
        }
      }
    }
    for (final NodeAddress node : neighborsToRemove) {
      verficationNonces.remove(node);
    }
    // ========================
    // final Map<NodeSpecificImplementation, LinkedList<Long>>
    // newNeighborsNeighborToNonces = new TreeMap<NodeSpecificImplementation,
    // LinkedList<Long>>();
    // for (final NodeSpecificImplementation node : verficationNonces.keySet())
    // {
    // if (neighborsNeighborToNonces.get(node) != null &&
    // neighborsNeighborToNonces.get(node).size() > 0) {
    // newNeighborsNeighborToNonces.put(node,
    // neighborsNeighborToNonces.get(node));
    // }
    // }
    // neighborsNeighborToNonces.clear();
    // neighborsNeighborToNonces.putAll(newNeighborsNeighborToNonces);
    return null;
  }
  
  public Map<NodeAddress, Nonce> getNextNonces() {
    final Map<NodeAddress, Nonce> nextNonces = new TreeMap<NodeAddress, Nonce>();
    final LinkedList<NodeAddress> nodesToRemove = new LinkedList<NodeAddress>();
    for (final NodeAddress node : neighborsNeighborToNonces.keySet()) {
      nextNonces.put(node, neighborsNeighborToNonces.get(node).removeFirst());
      if (neighborsNeighborToNonces.get(node).size() <= 0) {
        nodesToRemove.add(node);
      }
    }
    for (final NodeAddress node : nodesToRemove) {
      neighborsNeighborToNonces.remove(node);
    }
    return nextNonces;
  }
  
  @Override public String toString() {
    return "DNVPNonces: " + neighborsNeighborToNonces.toString();
  }
  
  public Set<NodeAddress> getNeighborsToReproduce(final Long currentRound) {
    final Set<NodeAddress> neighborsToReproduce = new TreeSet<NodeAddress>();
    for (final NodeAddress node : neighborsNeighborToRoundIssuedNonces.keySet()) {
      if ((neighborsNeighborToNonces.get(node).size() < numOfNoncesToProduce * 2)
          && (neighborsNeighborToRoundIssuedNonces.get(node) + numOfNoncesToProduce <= currentRound + 2)) {
        neighborsToReproduce.add(node);
      }
    }
    return neighborsToReproduce;
  }
  
  public void generateAndSendNonces(final NodeAddress nodeToDeliverTo, final NodeAddress neighborNeighbor, final Long currentRound) {
    final LinkedList<Nonce> newNonces = DNVPNonces.generateNonces(numOfNoncesToProduce, r);
    addNoncesForNeighbor(neighborNeighbor, newNonces);
    neighborsNeighborToRoundIssuedNonces.put(neighborNeighbor, currentRound);
    client.network
        .send(new NewNoncesMessage(messageTag, client.network.getAddress(), neighborNeighbor, newNonces, nodeToDeliverTo));
    client.network.send(new NoncesDelivered(messageTag, client.network.getAddress(), nodeToDeliverTo, neighborNeighbor,
        currentRound));
  }
  
  public void removeNoncesOfExNeighbors(final Set<NodeAddress> currentNeighbors) {
    final Set<NodeAddress> neighborsToRemove = new TreeSet<NodeAddress>();
    neighborsToRemove.addAll(neighborsNeighborToNonces.keySet());
    neighborsToRemove.removeAll(currentNeighbors);
    removeNeighborsAndItsNonces(neighborsToRemove);
  }
  
  // private Set<NodeSpecificImplementation>
  // getNeighborNeighborsThatNoncesAreExpectedFrom(final Long currentRound) {
  // final Set<NodeSpecificImplementation> nodes = new
  // TreeSet<NodeSpecificImplementation>();
  // for (final Entry<NodeSpecificImplementation, Long> entry :
  // neighborsNeighborToRoundIssuedNonces.entrySet()) {
  // if (entry.getValue() + 2 < currentRound) {
  // nodes.add(entry.getKey());
  // }
  // }
  // return nodes;
  // }
  public void limitNoncesForNeighbor(final NodeAddress node, final double factor) {
    if (neighborsNeighborToNonces.get(node) == null) {
      return;
    }
    final int numOfNoncesToSave = (int) (numOfNoncesToProduce * factor);
    if (neighborsNeighborToNonces.get(node).size() <= numOfNoncesToSave) {
      return;
    }
    neighborsNeighborToNonces.put(node, new LinkedList<Nonce>(neighborsNeighborToNonces.get(node).subList(0, numOfNoncesToSave)));
  }
}
