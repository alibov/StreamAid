package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.CoolStreamingPlus.ExchangeBitMapMessage;
import messages.CoolStreamingPlus.MembershipMessage;
import messages.CoolStreamingPlus.PartnershipDropping;
import messages.CoolStreamingPlus.PartnershipRequestApprovedMessage;
import messages.CoolStreamingPlus.PartnershipRequestApprovedMessageaAck;
import messages.CoolStreamingPlus.PartnershipRequestDeclinedMessage;
import messages.CoolStreamingPlus.PartnershipRequestMessage;
import modules.P2PClient;
import utils.Utils;
import utils.mCache;
import entites.PartnershipManager;
import experiment.frameworks.NodeAddress;

public class CoolStreamingPlusOverlay extends MultipleTreeOverlayModule<partnerInfo> {
  class Parent {
    NodeAddress parentNode;
    int coolDown;
    boolean isNew;
    
    public Parent() {
      parentNode = null;
      coolDown = 0;
      isNew = false;
    }
  }
  
  private final int M; // max size of cache
  private int numOfContacts = 0;
  private final double contactRatio;
  private final int exploreRound;
  private final int bitMapTimeout;
  private final int numberOfSubstreams;
  private final int ta;
  private final int ts;
  private final int tp;
  private final mCache cache;
  private Boolean oneTimeBootStrap = true;
  private final P2PClient client;
  // DEBUG
  int numOfCycle = 0;
  //
  private int bitMapCounter = 1;
  private final Set<NodeAddress> newPartners = new HashSet<NodeAddress>();
  /** newPartners are partners that didn't send me a bit map yet **/
  private final PartnershipManager partnershipManger;
  private final Parent[] parents;
  private final Boolean[] isParnetExist;
  
  /** it is only one possibility of how to save partners **/
  public CoolStreamingPlusOverlay(final P2PClient client, final int M, final int numberOfSubstreams, final int exploreRound,
      final int bitMapTimeout, final double contactRatio, final int ta, final int ts, final int tp,
      final int bitMapCyclesForPartnerToBeSufficient, final Random r) {
    super(client, r);
    this.client = client;
    addIngredient(new RandomGroupBootstrapIngredient(M, new Random(r.nextLong())), client);
    this.M = M;
    this.numberOfSubstreams = numberOfSubstreams;
    this.exploreRound = exploreRound;
    this.contactRatio = contactRatio;
    this.bitMapTimeout = bitMapTimeout;
    this.ta = ta;
    this.tp = tp;
    this.ts = ts;
    cache = new mCache(M, network.getAddress(), new Random(r.nextLong()));
    isParnetExist = new Boolean[numberOfSubstreams];
    for (int i = 0; i < isParnetExist.length; i++) {
      isParnetExist[i] = false;
    }
    parents = new Parent[numberOfSubstreams];
    for (int i = 0; i < parents.length; i++) {
      parents[i] = new Parent();
    }
    partnershipManger = new PartnershipManager(client, numberOfSubstreams, (int) (contactRatio * M), ts, tp, bitMapTimeout,
        bitMapCyclesForPartnerToBeSufficient, isParnetExist);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    partnershipManger.nextCycle();
    partnershipManger.clearUnsuitablePartners();
    //
    if (!network.isServerMode()) {
      parentsManagement();
    } else {
      if (client.player.getVs() != null && client.player.getVs().getLatestChunk() != null) {
        final Set<Long> set = new HashSet<Long>();
        set.add(client.player.getVs().getLatestChunk().index);
        partnershipManger.getBml().onStreamUpdate(new HashSet<Long>(set));
      }
    }
    partnersManagement();
    bitMapManagement();
    //
    //
    // printState();
    numOfCycle++;
  }
  
  private void printState() {
    System.out.print(numOfCycle + " : " + network.getAddress().getName() + " : mC" + cache.toString() + " : Partners"
        + partnershipManger.toString());
    //
    System.out.print(" Parents [ ");
    for (final Parent parent : parents) {
      if (parent != null && parent.parentNode != null) {
        System.out.print(parent.parentNode.getName() + ", ");
      }
    }
    System.out.print("] ");
    //
    System.out.print(" ------------CHUNKS");
    System.out.print(partnershipManger.bml.toString());
    //
    System.out.print(" Childs [ ");
    for (int i = 0; i < numberOfSubstreams; i++) {
      System.out.print("{");
      for (final NodeAddress node : getSonNodes(i)) {
        System.out.print(node.getName() + "|");
      }
      System.out.print("}, ");
    }
    System.out.println(" ]");
  }
  
  /***
   * this function returns the second part of the bit map - what I want to get
   * from my partner/parent
   *
   * @param network
   *          - the node of my partner that I'm going to contact.
   * @return
   */
  private Long[] getSecondPartOfBitmap(final NodeAddress partner) {
    final Long[] secondPartOfBitmap = new Long[numberOfSubstreams];
    int i = 0;
    for (final Parent p : parents) {
      if (p.parentNode == partner && p.isNew) {
        secondPartOfBitmap[i] = (long) 1;
        p.isNew = false;
      } else {
        secondPartOfBitmap[i] = (long) 0;
      }
      i++;
    }
    return secondPartOfBitmap;
  }
  
  /***
   * this function initialize array to 0
   *
   * @param arr
   */
  private static void initArray(final Long[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = (long) 0;
    }
  }
  
  private void bitMapManagement() {
    if (bitMapCounter % bitMapTimeout == 0) {
      Long[] secondPartOfBitmap = new Long[numberOfSubstreams];
      Long[] bitMap = new Long[2 * numberOfSubstreams];
      //
      for (final NodeAddress partner : partnershipManger.getCurrentPartners()) {
        secondPartOfBitmap = getSecondPartOfBitmap(partner);
        bitMap = partnershipManger.produceBitmap(secondPartOfBitmap);
        network.send(new ExchangeBitMapMessage(bitMap, getMessageTag(), network.getAddress(), partner));
      }
      //
      initArray(secondPartOfBitmap);
      for (final NodeAddress newPartner : newPartners) {
        bitMap = partnershipManger.produceBitmap(secondPartOfBitmap);
        network.send(new ExchangeBitMapMessage(bitMap, getMessageTag(), network.getAddress(), newPartner));
      }
    }
    bitMapCounter++;
  }
  
  /**
   * picks partners from m-cache and sends them partnership requests requests
   */
  private void partnersManagement() {
    numOfContacts = newPartners.size() + partnershipManger.getNumOfPartners();
    final int maxNumOfPartners = (int) (M * contactRatio);
    //
    if (numOfContacts < maxNumOfPartners && cache.size() > 0) {
      int numOfPartnersMissing = maxNumOfPartners - numOfContacts;
      //
      if (numOfPartnersMissing > cache.size()) {
        numOfPartnersMissing = cache.size();
      }
      //
      final Set<NodeAddress> currentPartners = new HashSet<NodeAddress>(newPartners);
      currentPartners.addAll(partnershipManger.getCurrentPartners());
      //
      final Set<NodeAddress> contactNodes = new HashSet<NodeAddress>();
      cache.getRandomNodes(contactNodes, numOfPartnersMissing, currentPartners);
      //
      for (final NodeAddress i : contactNodes) {
        network.send(new PartnershipRequestMessage<Sizeable>(getMessageTag(), network.getAddress(), i));
      }
    }
  }
  
  /***
   * this function deals with parenthood. If parent needs to be removed it
   * removes them. if parents needs to be selected it selects them.
   */
  private void parentsManagement() {
    int substream = 0;
    for (final Parent p : parents) {
      final Collection<NodeAddress> candidates = partnershipManger.getSuitableNodesForParenthoodInSubstream(substream, false);
      final Collection<NodeAddress> suitableParents = partnershipManger.getSuitableNodesForParenthoodInSubstream(substream, true);
      // if there is still cool-down do nothing
      if (p.coolDown != 0) {
        p.coolDown--;
      } else {
        // if there is a parent and the cool-down is 0, check if the parent is
        // still suitable. if not replace it
        if (p.parentNode != null) {
          if (!suitableParents.contains(p.parentNode)) {
            network.send(new PartnershipDropping(substream, getMessageTag(), network.getAddress(), p.parentNode));
            //
            if (candidates.isEmpty()) {
              p.parentNode = null;
              p.isNew = false;
              isParnetExist[substream] = false;
            } else {
              p.parentNode = Utils.pickRandomElement(candidates, r);
              p.isNew = true;
              p.coolDown = ta;
              isParnetExist[substream] = true;
            }
            return;
          }
        }
      }
      // if there is no parent, and there is a suitable parent for this
      // sub-stream
      if (p.parentNode == null && !candidates.isEmpty()) {
        p.parentNode = Utils.pickRandomElement(candidates, r);
        p.isNew = true;
        p.coolDown = ta;
        isParnetExist[substream] = true;
      }
      substream++;
    }
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    final int maxNumOfPartners = (int) (contactRatio * M);
    //
    //
    if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      if (oneTimeBootStrap) {
        oneTimeBootStrap = false;
        final SeedNodeMultipleTargetsReplyMessage msg = (SeedNodeMultipleTargetsReplyMessage) message;
        final Collection<NodeAddress> targets = msg.targets;
        final Set<NodeAddress> bootNodes = new HashSet<NodeAddress>(targets);
        cache.insertCollectionOfNodes(bootNodes);
      }
    }
    //
    //
    else if (message instanceof MembershipMessage) {
      final MembershipMessage msg = (MembershipMessage) message;
      cache.insertCollectionOfNodes(msg.nodes);
    }
    //
    //
    else if (message instanceof PartnershipRequestMessage) {
      if (maxNumOfPartners > numOfContacts) {
        network.send(new PartnershipRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), message.sourceId));
      } else {
        network.send(new PartnershipRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
      }
    }
    //
    //
    else if (message instanceof PartnershipRequestApprovedMessage) {
      if (maxNumOfPartners <= numOfContacts) {
        network.send(new PartnershipRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
        return;
      }
      //
      newPartners.add(message.sourceId);
      numOfContacts++;
      final Set<NodeAddress> nodesToSend = new HashSet<NodeAddress>();
      cache.toCollection(nodesToSend);
      network.send(new MembershipMessage(nodesToSend, getMessageTag(), network.getAddress(), message.sourceId));
      network.send(new PartnershipRequestApprovedMessageaAck(getMessageTag(), network.getAddress(), message.sourceId));
    }
    //
    //
    else if (message instanceof PartnershipRequestApprovedMessageaAck) {
      if (maxNumOfPartners <= numOfContacts) {
        network.send(new PartnershipRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
        return;
      }
      //
      newPartners.add(message.sourceId);
      numOfContacts++;
      final Set<NodeAddress> nodesToSend = new HashSet<NodeAddress>();
      cache.toCollection(nodesToSend);
      network.send(new MembershipMessage(nodesToSend, getMessageTag(), network.getAddress(), message.sourceId));
    }
    //
    //
    else if (message instanceof PartnershipRequestDeclinedMessage) {
      if (newPartners.contains(message.sourceId)) {
        newPartners.remove(message.sourceId);
      }
      //
      numOfContacts--;
      if (partnershipManger.getCurrentPartners().contains(message.sourceId)) {
        partnershipManger.removePartner(message.sourceId);
      }
    }
    //
    //
    else if (message instanceof ExchangeBitMapMessage) {
      final ExchangeBitMapMessage msg = (ExchangeBitMapMessage) message;
      partnershipManger.updateBitmapToPartner(msg.sourceId, msg.bitMap);
      //
      checkSubscriptionForSubstream(msg.bitMap, msg.sourceId);
      if (newPartners.contains(msg.sourceId)) {
        newPartners.remove(msg.sourceId);
      }
    }
    //
    //
    else if (message instanceof PartnershipDropping) {
      final PartnershipDropping msg = (PartnershipDropping) message;
      descriptorToSons.get(msg.substream).remove(msg.sourceId);
    }
  }
  
  private void checkSubscriptionForSubstream(final Long[] bitMap, final NodeAddress sourceId) {
    for (int i = 0; i < numberOfSubstreams; i++) {
      if (bitMap[i + numberOfSubstreams] == 1) {
        if (descriptorToSons.get(i) == null) {
          descriptorToSons.put(i, new HashSet<NodeAddress>());
        }
        descriptorToSons.get(i).add(sourceId);
      }
    }
  }
}
