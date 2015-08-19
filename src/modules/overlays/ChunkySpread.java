package modules.overlays;

import interfaces.Sizeable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.AssertionFailedError;
import messages.ChunkMessage;
import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestDeclinedWithPayload;
import messages.ConnectionRequestMessage;
import messages.Message;
import messages.chunkySpread.AdoptRequest;
import messages.chunkySpread.AdoptRequestAccept;
import messages.chunkySpread.AdoptRequestDecline;
import messages.chunkySpread.BeginParentSwap;
import messages.chunkySpread.BloomFilterUpdate;
import messages.chunkySpread.FastNeighborsOffer;
import messages.chunkySpread.FloodTree;
import messages.chunkySpread.NeighborLatencyChange;
import messages.chunkySpread.NeighborLoadChange;
import messages.chunkySpread.ParentOverloaded;
import messages.chunkySpread.RenounceParent;
import messages.chunkySpread.UnderLoadedNeighborsOffer;
import modules.P2PClient;
import utils.Common;
import utils.chunkySpread.LatencyMeasure;
import utils.chunkySpread.LatencyMeasureFactory;
import utils.chunkySpread.LatencyState;
import utils.chunkySpread.LoadState;
import utils.chunkySpread.NodeSpecImplFunnel;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public class ChunkySpread extends MultipleTreeOverlayModule<Object> {
  private final OverlayModule<?> swaplinks;
  private final int treesNum;
  // how long to wait before initiaing churn pattern
  private final int noParentTimeout;
  private int currNoParentTimeout;
  // how long to wait before assuming answer is lost
  private final int pendingTimeout;
  private final List<Integer> currPendingTimeout;
  // in order to let swaplinks to init his graph before chunky kicks in
  private int startDelay;
  // wait for flood to arrive before initiaing churn pattern
  private int churnDelay;
  private boolean isInitialized = false;
  private final Map<Integer, NodeAddress> parents;
  private int currLoad = 0;
  // between these load states we may tune latency and deem load state is fine
  private final int upperThreshold;
  private final int lowerThreshold;
  private final int maxLoad;
  private int allowedLoadParentSwap = 0;
  private int allowedLatencyParentSwap = 0;
  private LoadState loadState = LoadState.WELL;
  private final Map<Integer, Queue<NodeAddress>> potentialFloodParents;
  private final ArrayList<NodeAddress> underLoadedNeighbors;
  private final int expectedNodesNum;
  // bloom filters to hold for each tree the assumed path from the root to us in
  // order to prevent loops
  private final Map<Integer, BloomFilter<NodeAddress>> bloomFilter;
  private final LatencyMeasure latencyMeasurer;
  // tells for each tree how relatively fast is it
  private Map<Integer, LatencyState> treesLatenices;
  private final Map<Integer, Set<NodeAddress>> fastNeighbors;
  private final Map<Integer, NodeAddress> slowParents;
  private final boolean DEBUG = false;
  
  public ChunkySpread(final P2PClient client, final OverlayModule<?> _swaplinks, final int _maxLoad, final double ULT,
      final double LLT, final int _startDelay, final int _churnDelay, final int _noParentTimeout, final int _pendingTimeout,
      final LatencyMeasureFactory factory, final Random r) {
    super(client, r);
    network.addListener(this, ChunkMessage.class);
    treesNum = Common.currentConfiguration.descriptions;
    expectedNodesNum = Common.currentConfiguration.nodes;
    maxLoad = _maxLoad;
    upperThreshold = (int) (ULT * maxLoad);
    lowerThreshold = (int) (LLT * maxLoad);
    startDelay = _startDelay;
    churnDelay = _churnDelay;
    noParentTimeout = _noParentTimeout;
    currNoParentTimeout = noParentTimeout;
    pendingTimeout = _pendingTimeout;
    swaplinks = _swaplinks;
    potentialFloodParents = new TreeMap<Integer, Queue<NodeAddress>>();
    for (int treeID = 0; treeID < treesNum; treeID++) {
      potentialFloodParents.put(treeID, new LinkedList<NodeAddress>());
    }
    parents = new TreeMap<Integer, NodeAddress>();
    bloomFilter = new TreeMap<Integer, BloomFilter<NodeAddress>>();
    underLoadedNeighbors = new ArrayList<NodeAddress>();
    currPendingTimeout = new ArrayList<Integer>();
    for (int treeID = 0; treeID < treesNum; treeID++) {
      currPendingTimeout.add(-1);
    }
    latencyMeasurer = factory.create();
    fastNeighbors = new TreeMap<Integer, Set<NodeAddress>>();
    for (int i = 0; i < treesNum; i++) {
      fastNeighbors.put(i, new TreeSet<NodeAddress>());
    }
    slowParents = new TreeMap<Integer, NodeAddress>();
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    swaplinks.setServerMode();
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof FloodTree) {
      final FloodTree msg = (FloodTree) message;
      handleMessage(msg);
      return;
    } else if (message instanceof ConnectionRequestApprovedMessage<?>) {
      final ConnectionRequestApprovedMessage<?> msg = (ConnectionRequestApprovedMessage<?>) message;
      handleMessage(msg);
      return;
    } else if (message instanceof ConnectionRequestDeclinedWithPayload<?>) {
      final ConnectionRequestDeclinedWithPayload<?> msg = (ConnectionRequestDeclinedWithPayload<?>) message;
      handleMessage(msg);
    } else if (message instanceof ConnectionRequestMessage<?>) {
      final ConnectionRequestMessage<?> msg = (ConnectionRequestMessage<?>) message;
      handleMessage(msg);
      return;
    } else if (message instanceof BloomFilterUpdate) {
      final BloomFilterUpdate msg = (BloomFilterUpdate) message;
      handleMessage(msg);
      return;
    } else if (message instanceof ParentOverloaded) {
      final ParentOverloaded msg = (ParentOverloaded) message;
      handleMessage(msg);
      return;
    } else if (message instanceof NeighborLoadChange) {
      final NeighborLoadChange msg = (NeighborLoadChange) message;
      handleMessage(msg);
      return;
    } else if (message instanceof UnderLoadedNeighborsOffer) {
      final UnderLoadedNeighborsOffer msg = (UnderLoadedNeighborsOffer) message;
      handleMessage(msg);
      return;
    } else if (message instanceof BeginParentSwap) {
      final BeginParentSwap msg = (BeginParentSwap) message;
      handleMessage(msg);
    } else if (message instanceof AdoptRequest) {
      final AdoptRequest msg = (AdoptRequest) message;
      handleMessage(msg);
    } else if (message instanceof AdoptRequestAccept) {
      final AdoptRequestAccept msg = (AdoptRequestAccept) message;
      handleMessage(msg);
    } else if (message instanceof AdoptRequestDecline) {
      final AdoptRequestDecline msg = (AdoptRequestDecline) message;
      handleMessage(msg);
    } else if (message instanceof RenounceParent) {
      final RenounceParent msg = (RenounceParent) message;
      handleMessage(msg);
    } else if (message instanceof ChunkMessage) {
      final ChunkMessage msg = (ChunkMessage) message;
      handleMessage(msg);
    } else if (message instanceof NeighborLatencyChange) {
      final NeighborLatencyChange msg = (NeighborLatencyChange) message;
      handleMessage(msg);
    } else if (message instanceof FastNeighborsOffer) {
      final FastNeighborsOffer msg = (FastNeighborsOffer) message;
      handleMessage(msg);
    }
  }
  
  private void handleMessage(final FloodTree msg) {
    final int treeID = msg.getTreeID();
    if (parents.containsKey(treeID)) {
      return;
    }
    potentialFloodParents.get(treeID).add(msg.sourceId);
    // if not pending another connection request
    if (currPendingTimeout.get(treeID) == -1) {
      sendConnectionRequestMessage(treeID, new FloodInfo(msg.getTreeID(), null, false));
    }
  }
  
  private void handleMessage(final BloomFilterUpdate msg) {
    if (!parents.containsKey(msg.getTreeID()) || !parents.get(msg.getTreeID()).equals(msg.sourceId)) {
      return;
    }
    // if there might be a circle, renounce parent
    if (!inheritBloomFilterAndAddSelf(msg.getTreeID(), msg.getBloomFilter())) {
      network.send(new RenounceParent(getMessageTag(), network.getAddress(), parents.get(msg.getTreeID()), msg.getTreeID(), null,
          true));
      parents.remove(msg.getTreeID());
      currNoParentTimeout = 0;
      return;
    }
    // continue the flood to tree children
    sendBloomFilterToAllSons(msg.getTreeID());
  }
  
  public void sendBloomFilterToAllSons(final int treeID) {
    for (final NodeAddress son : getSonNodes(treeID, false)) {
      network.send(new BloomFilterUpdate(getMessageTag(), network.getAddress(), son, treeID, bloomFilter.get(treeID).copy()));
    }
  }
  
  private void handleMessage(final ConnectionRequestMessage<?> msg) {
    final int treeID = ((FloodInfo) (msg.payload)).getTreeID();
    final boolean isChurn = ((FloodInfo) (msg.payload)).isChurn();
    // if parenting the message source is not valid for me
    if ((parents.get(treeID) == null && !network.isServerMode()) || bloomFilter.get(treeID).mightContain(msg.sourceId)
        || currLoad >= maxLoad) {
      network.send(new ConnectionRequestDeclinedWithPayload<FloodInfo>(getMessageTag(), network.getAddress(), msg.sourceId,
          new FloodInfo(treeID, null, isChurn)));
    } else {
      addSonToTree(treeID, msg.sourceId);
      network.send(new ConnectionRequestApprovedMessage<FloodInfo>(getMessageTag(), network.getAddress(), msg.sourceId,
          new FloodInfo(treeID, bloomFilter.get(treeID).copy(), isChurn)));
    }
  }
  
  private void handleMessage(final ConnectionRequestDeclinedWithPayload<?> msg) {
    final int treeID = ((FloodInfo) (msg.payload)).getTreeID();
    final boolean isChurn = ((FloodInfo) (msg.payload)).isChurn();
    // already got a parent, so there's no need keep looking
    if (parents.get(treeID) != null) {
      return;
    }
    potentialFloodParents.get(treeID).remove();
    removeDeadPotentialParents(treeID);
    // if the are no potential parents left, fill with neighbors
    if (potentialFloodParents.get(treeID).isEmpty()) {
      addNeighborsToPParents(treeID);
    }
    setNotPending(treeID);
    if (!potentialFloodParents.get(treeID).isEmpty()) {
      sendConnectionRequestMessage(treeID, new FloodInfo(treeID, null, isChurn));
    }
  }
  
  private void handleMessage(final ConnectionRequestApprovedMessage<?> msg) {
    final BloomFilter<NodeAddress> bf = ((FloodInfo) (msg.payload)).getBloomFilter();
    final int treeID = ((FloodInfo) (msg.payload)).getTreeID();
    final boolean isChurn = ((FloodInfo) (msg.payload)).isChurn();
    // for simultaneous approves
    if (parents.get(treeID) != null) {
      network.send(new RenounceParent(getMessageTag(), network.getAddress(), msg.sourceId, treeID, null, true));
      return;
    }
    setNotPending(treeID);
    potentialFloodParents.get(treeID).clear();
    currNoParentTimeout = noParentTimeout;
    debugPrint(network.getAddress() + " became son of " + msg.sourceId + " in tree " + treeID);
    // set the bloom filter to be the same as the parent's and add ourselves
    inheritBloomFilterAndAddSelf(treeID, bf);
    parents.put(treeID, msg.sourceId);
    if (isChurn) {
      return;
    }
    // continue the flood to all swaplinks neighbors
    final Collection<NodeAddress> swapLinksNeighbors = swaplinks.getNeighbors();
    for (final NodeAddress neighbor : swapLinksNeighbors) {
      // avoid flooding the root (server)
      if (network.getServerNode().equals(neighbor)) {
        continue;
      }
      // we dont send back
      if (neighbor.equals(msg.sourceId)) {
        continue;
      }
      network.send(new FloodTree(getMessageTag(), network.getAddress(), neighbor, treeID, bloomFilter.get(treeID).copy()));
    }
  }
  
  private void setNotPending(final int treeID) {
    currPendingTimeout.set(treeID, -1);
  }
  
  private void handleMessage(final NeighborLoadChange msg) {
    if (LoadState.UNDER == msg.getToState()) {
      underLoadedNeighbors.add(msg.sourceId);
    } else { // WELL or OVER
      if (underLoadedNeighbors.contains(msg.sourceId)) {
        underLoadedNeighbors.remove(msg.sourceId);
      }
    }
  }
  
  private void handleMessage(final ParentOverloaded msg) {
    // if source is no longer my parent
    if (parents.get(msg.getTreeID()) == null || !parents.get(msg.getTreeID()).equals(msg.sourceId)) {
      return;
    }
    sendUnderLoadedNeighborsOffer(msg.sourceId, msg.getTreeID());
  }
  
  private void sendUnderLoadedNeighborsOffer(final NodeAddress sourceId, final int treeID) {
    final ArrayList<NodeAddress> underLoadedNeighborsCopy = new ArrayList<NodeAddress>();
    underLoadedNeighborsCopy.addAll(underLoadedNeighbors);
    if (underLoadedNeighborsCopy.isEmpty()) {
      return;
    }
    network.send(new UnderLoadedNeighborsOffer(getMessageTag(), network.getAddress(), sourceId, treeID, underLoadedNeighborsCopy));
  }
  
  private void handleMessage(final UnderLoadedNeighborsOffer msg) {
    if (loadState != LoadState.OVER || allowedLoadParentSwap <= 0) {
      return;
    }
    allowedLoadParentSwap--;
    // address to a random adopter
    Collections.shuffle(msg.getOfferedParents(), r);
    final NodeAddress adopter = msg.getOfferedParents().remove(0);
    network.send(new BeginParentSwap(getMessageTag(), network.getAddress(), msg.sourceId, msg.getTreeID(), adopter, true));
  }
  
  private void handleMessage(final FastNeighborsOffer msg) {
    if (treesLatenices.get(msg.getTreeID()) != LatencyState.SLOW || loadState != LoadState.WELL || allowedLatencyParentSwap <= 0) {
      return;
    }
    allowedLatencyParentSwap--;
    // address to a random adopter
    final List<NodeAddress> randomFastNeighbors = new ArrayList<NodeAddress>(msg.getFastNeighbors());
    Collections.shuffle(randomFastNeighbors, r);
    final NodeAddress adopter = randomFastNeighbors.remove(0);
    debugPrint(network.getAddress() + " instructs " + msg.sourceId + " to latency-swap to " + adopter);
    network.send(new BeginParentSwap(getMessageTag(), network.getAddress(), msg.sourceId, msg.getTreeID(), adopter, false));
  }
  
  public void handleMessage(final BeginParentSwap msg) {
    final int treeID = msg.getTreeID();
    // if the source is no longer my parent
    if (parents.get(treeID) == null || !parents.get(treeID).equals(msg.sourceId)) {
      return;
    }
    network.send(new AdoptRequest(getMessageTag(), network.getAddress(), msg.getThirdParty(), treeID, msg.sourceId, null, msg
        .isLoadBalancing()));
  }
  
  private void handleMessage(final AdoptRequest msg) {
    // if adopting is no valid
    if (!parents.containsKey(msg.getTreeID()) || bloomFilter.get(msg.getTreeID()).mightContain(msg.sourceId)
        || currLoad + 1 > upperThreshold) {
      if (!msg.isLoadBalancing()) {
        debugPrint(network.getAddress() + " refused to latency-adopt " + msg.sourceId);
      }
      network.send(new AdoptRequestDecline(getMessageTag(), network.getAddress(), msg.sourceId, msg.getTreeID(), msg
          .getThirdParty(), msg.isLoadBalancing()));
    } else {
      if (!msg.isLoadBalancing()) {
        debugPrint(network.getAddress() + " agreed to latency-adopt " + msg.sourceId);
      }
      addSonToTree(msg.getTreeID(), msg.sourceId);
      network.send(new AdoptRequestAccept(getMessageTag(), network.getAddress(), msg.sourceId, msg.getTreeID(),
          msg.getThirdParty(), bloomFilter.get(msg.getTreeID()).copy(), msg.isLoadBalancing()));
    }
  }
  
  private void handleMessage(final AdoptRequestAccept msg) {
    debugPrint(network.getAddress() + " swapped to " + msg.sourceId + " from " + msg.getThirdParty() + " in tree "
        + msg.getTreeID() + " (" + (msg.isLoadBalancing() ? "load" : "latency") + ")");
    if (parents.get(msg.getTreeID()) != null) {
      // to prevent 2 swaps from the same parent
      if (!network.getServerNode().equals(msg.sourceId) && !parents.get(msg.getTreeID()).equals(msg.getThirdParty())) {
        network.send(new RenounceParent(getMessageTag(), network.getAddress(), msg.sourceId, msg.getTreeID(), parents.get(msg
            .getTreeID()), msg.isLoadBalancing()));
        return;
      }
      network.send(new RenounceParent(getMessageTag(), network.getAddress(), parents.get(msg.getTreeID()), msg.getTreeID(),
          msg.sourceId, msg.isLoadBalancing()));
    }
    inheritBloomFilterAndAddSelf(msg.getTreeID(), msg.getBloomFilter());
    parents.put(msg.getTreeID(), msg.sourceId);
    currNoParentTimeout = noParentTimeout;
    sendBloomFilterToAllSons(msg.getTreeID());
  }
  
  private void handleMessage(final AdoptRequestDecline msg) {
    if (parents.get(msg.getTreeID()) == null) {
      return;
    }
    if (msg.isLoadBalancing()) {
      sendUnderLoadedNeighborsOffer(parents.get(msg.getTreeID()), msg.getTreeID());
    }
  }
  
  private boolean inheritBloomFilterAndAddSelf(final int treeID, final BloomFilter<NodeAddress> toBeInherited) {
    final boolean res = !toBeInherited.mightContain(network.getAddress());
    toBeInherited.put(network.getAddress());
    bloomFilter.put(treeID, toBeInherited);
    return res;
  }
  
  private void handleMessage(final RenounceParent msg) {
    debugPrint(network.getAddress() + " stopped parenting son " + msg.sourceId);
    removeSonFromTree(msg.getTreeID(), msg.sourceId);
  }
  
  private void handleMessage(final ChunkMessage msg) {
    latencyMeasurer.putChunk(msg.chunk);
  }
  
  private void handleMessage(final NeighborLatencyChange msg) {
    // update fastNeighbors
    final Map<Integer, LatencyState> neighborLatencies = msg.getTreesLatencies();
    for (int i = 0; i < treesNum; i++) {
      if (msg.sourceId.equals(parents.get(i))) {
        continue;
      }
      if (LatencyState.FAST.equals(neighborLatencies.get(i))) {
        fastNeighbors.get(i).add(msg.sourceId);
      } else {
        fastNeighbors.get(i).remove(msg.sourceId);
      }
    }
    // in which trees parent slow?
    for (int i = 0; i < treesNum; i++) {
      if (msg.sourceId.equals(parents.get(i))) {
        if (neighborLatencies.get(i) == LatencyState.SLOW) {
          slowParents.put(i, msg.sourceId);
        } else {
          slowParents.remove(i);
        }
      }
    }
  }
  
  public void addSonToTree(final int treeID, final NodeAddress son) {
    getSonNodes(treeID, false).add(son);
    currLoad++;
  }
  
  public void removeSonFromTree(final int treeID, final NodeAddress son) {
    getSonNodes(treeID, false).remove(son);
    currLoad--;
  }
  
  @Override public void nextCycle() {
    if (network.isServerMode()) {
      debugPrint("---------- Cycle -----------");
    }
    super.nextCycle();
    swaplinks.nextCycle();
    if (startDelay > 0) {
      startDelay--;
      return;
    }
    if (!swaplinks.isOverlayConnected()) {
      return;
    }
    if (!isInitialized && network.isServerMode()) {
      // Bloom filters initialization
      for (int i = 0; i < treesNum; i++) {
        final BloomFilter<NodeAddress> bf = BloomFilter.<NodeAddress> create(new NodeSpecImplFunnel(), expectedNodesNum);
        bf.put(network.getAddress());
        bloomFilter.put(i, bf);
      }
      final Set<NodeAddress> swapLinksNeighbors = swaplinks.getNeighbors();
      int i = 0;
      // assign a slice source for each tree
      while (i < treesNum) {
        for (final NodeAddress neighbor : swapLinksNeighbors) {
          if (i >= treesNum) {
            break;
          }
          network.send(new FloodTree(getMessageTag(), network.getAddress(), neighbor, i, bloomFilter.get(i).copy()));
          i++;
        }
      }
      isInitialized = true;
    }
    if (!network.isServerMode()) {
      // remove dead sons
      for (int treeID = 0; treeID < treesNum; treeID++) {
        final Collection<NodeAddress> noLongerMySons = new ArrayList<NodeAddress>();
        for (final NodeAddress son : getSonNodes(treeID, false)) {
          if (!network.isUp(son)) {
            noLongerMySons.add(son);
          }
        }
        for (final NodeAddress son : noLongerMySons) {
          removeSonFromTree(treeID, son);
        }
      }
      // Pushing load state only on status change
      if (currLoad < lowerThreshold && loadState != LoadState.UNDER) {
        debugPrint(network.getAddress() + " became UNDER loaded");
        loadState = LoadState.UNDER;
        sendLoadStateToAll();
      }
      if (currLoad >= lowerThreshold && currLoad <= upperThreshold && loadState != LoadState.WELL) {
        debugPrint(network.getAddress() + " became WELL loaded");
        loadState = LoadState.WELL;
        sendLoadStateToAll();
      }
      if (currLoad > upperThreshold && loadState != LoadState.OVER) {
        debugPrint(network.getAddress() + " became OVER loaded");
        loadState = LoadState.OVER;
        sendLoadStateToAll();
      }
      // each cycle we re-negotiate in order to prevent paralysis from parent
      // switch failures
      if (loadState == LoadState.OVER) {
        allowedLoadParentSwap = currLoad - upperThreshold;
        sendLoadStateToChildren();
      }
      // send bloomFilter to sons
      for (int treeID = 0; treeID < treesNum; treeID++) {
        for (final NodeAddress son : getSonNodes(treeID, false)) {
          network.send(new BloomFilterUpdate(getMessageTag(), network.getAddress(), son, treeID, bloomFilter.get(treeID).copy()));
        }
      }
      allowedLatencyParentSwap = currLoad - lowerThreshold;
      // send trees latency to all neighbors
      treesLatenices = latencyMeasurer.treesLatencies();
      for (final NodeAddress neigh : swaplinks.getNeighbors()) {
        network.send(new NeighborLatencyChange(getMessageTag(), network.getAddress(), neigh, new TreeMap<Integer, LatencyState>(
            treesLatenices)));
      }
      // ask parents the permission to swap
      for (final int i : slowParents.keySet()) {
        if (!fastNeighbors.get(i).isEmpty() && slowParents.get(i) != null && slowParents.get(i).equals(parents.get(i))) {
          debugPrint(network.getAddress() + ", son of " + parents.get(i) + " is SLOW in tree " + i);
          network.send(new FastNeighborsOffer(getMessageTag(), network.getAddress(), parents.get(i), i, new TreeSet<NodeAddress>(
              fastNeighbors.get(i))));
        }
      }
      slowParents.clear();
    }
    // Server checks whether all slice sources are alive
    if (network.isServerMode()) {
      for (int treeID = 0; treeID < treesNum; treeID++) {
        // never had a son in that tree
        if (getSonNodes(treeID, false).isEmpty()) {
          continue;
        }
        // Root has 1 son per tree, so we take the first from iterator
        final NodeAddress son = getSonNodes(treeID, false).iterator().next();
        if (!network.isUp(son)) {
          debugPrint("CHURN: " + "Root detected " + son + " isn't up");
          removeSonFromTree(treeID, son);
          final List<NodeAddress> shuffledNeighbors = new ArrayList<NodeAddress>(swaplinks.getNeighbors());
          Collections.shuffle(shuffledNeighbors);
          final NodeAddress sliceSource = shuffledNeighbors.remove(0);
          addSonToTree(treeID, sliceSource);
          network.send(new AdoptRequestAccept(getMessageTag(), network.getAddress(), sliceSource, treeID, null, bloomFilter.get(
              treeID).copy(), true));
        }
      }
    }
    // Handle churn
    else { // not root
      // Joined node churn
      if (churnDelay > 0) {
        churnDelay--;
        return;
      }
      if (!isOverlayConnected() && currNoParentTimeout > 0) {
        currNoParentTimeout--;
      } else if (!isOverlayConnected()) {
        currNoParentTimeout = noParentTimeout;
        for (int treeID = 0; treeID < treesNum; treeID++) {
          initPotentialFloodParent(treeID);
        }
      } else {
        currNoParentTimeout = noParentTimeout;
      }
      // Pending to parent answer churn
      for (int treeID = 0; treeID < treesNum; treeID++) {
        if (currPendingTimeout.get(treeID) < 0) {
          continue;
        }
        if (currPendingTimeout.get(treeID) < pendingTimeout) {
          currPendingTimeout.set(treeID, currPendingTimeout.get(treeID) + 1);
        } else {
          debugPrint("CHURN: " + network.getAddress() + "'s pending in tree " + treeID + " is over");
          setNotPending(treeID);
          potentialFloodParents.get(treeID).remove();
          initPotentialFloodParent(treeID);
        }
      }
    }
  }
  
  private void initPotentialFloodParent(final int treeID) {
    if (currPendingTimeout.get(treeID) >= 0 || (parents.get(treeID) != null && network.isUp(parents.get(treeID)))) {
      return;
    }
    parents.remove(treeID);
    debugPrint("CHURN: " + network.getAddress() + " has no parent in tree " + treeID);
    removeDeadPotentialParents(treeID);
    if (potentialFloodParents.get(treeID).isEmpty()) {
      addNeighborsToPParents(treeID);
    }
    sendConnectionRequestMessage(treeID, new FloodInfo(treeID, null, true));
  }
  
  private void addNeighborsToPParents(final int treeID) {
    final ArrayList<NodeAddress> shuffledNeighbors = new ArrayList<NodeAddress>(swaplinks.getNeighbors());
    Collections.shuffle(shuffledNeighbors, r);
    for (final NodeAddress neighbor : shuffledNeighbors) {
      if (network.getServerNode().equals(neighbor.toString())) {
        continue;
      }
      potentialFloodParents.get(treeID).add(neighbor);
    }
  }
  
  // DEBUG
  private void debugPrint(final String str) {
    if (DEBUG) {
      System.out.println(str);
    }
  }
  
  // DEBUG
  private boolean nodeMatch(final int n) {
    return network.getAddress().toString().equals(n + "");
  }
  
  @Override public boolean isOverlayConnected() {
    for (final NodeAddress parent : parents.values()) {
      if (!network.isUp(parent)) {
        return false;
      }
    }
    return parents.size() == treesNum;
  }
  
  private void sendConnectionRequestMessage(final int treeID, final FloodInfo fi) {
    removeDeadPotentialParents(treeID);
    if (potentialFloodParents.get(treeID).isEmpty()) {
      return;
    }
    setPending(treeID);
    network.send(new ConnectionRequestMessage<FloodInfo>(getMessageTag(), network.getAddress(), potentialFloodParents.get(treeID)
        .element(), fi));
  }
  
  private void setPending(final int treeID) {
    currPendingTimeout.set(treeID, 0);
  }
  
  private void removeDeadPotentialParents(final int treeID) {
    final ArrayList<NodeAddress> toBeRemoved = new ArrayList<NodeAddress>();
    for (final NodeAddress pParent : potentialFloodParents.get(treeID)) {
      if (!network.isUp(pParent)) {
        toBeRemoved.add(pParent);
      }
    }
    potentialFloodParents.get(treeID).removeAll(toBeRemoved);
  }
  
  public void sendLoadStateToAll() {
    final Collection<NodeAddress> swapLinksNeighbors = swaplinks.getNeighbors();
    for (final NodeAddress n : swapLinksNeighbors) {
      network.send(new NeighborLoadChange(getMessageTag(), network.getAddress(), n, loadState));
    }
  }
  
  public void sendLoadStateToChildren() {
    for (int treeID = 0; treeID < treesNum; treeID++) {
      final Set<NodeAddress> treeChildren = getSonNodes(treeID, false);
      for (final NodeAddress n : treeChildren) {
        network.send(new ParentOverloaded(getMessageTag(), network.getAddress(), n, treeID, loadState));
      }
    }
  }
  
  private void assertIsParent(final NodeAddress p) {
    if (!parents.values().contains(p)) {
      throw new AssertionFailedError("non-parent node!");
    }
  }
  
  @Override public Set<NodeAddress> getNeighbors() {
    final Set<NodeAddress> res = new TreeSet<NodeAddress>();
    for (int i = 0; i < treesNum; i++) {
      res.addAll(getSonNodes(i, false));
    }
    return res;
  }
  
  private class FloodInfo implements Sizeable {
    private final int treeID;
    private final BloomFilter<NodeAddress> bloomFilter;
    private final boolean isChurn;
    
    public BloomFilter<NodeAddress> getBloomFilter() {
      return bloomFilter;
    }
    
    public FloodInfo(final int _treeID, final BloomFilter<NodeAddress> _bloomFilter, final boolean _isChurn) {
      treeID = _treeID;
      bloomFilter = _bloomFilter;
      isChurn = _isChurn;
    }
    
    public int getTreeID() {
      return treeID;
    }
    
    public boolean isChurn() {
      return isChurn;
    }
    
    @Override public String toString() {
      return "Tree: " + treeID + (isChurn ? "(churn)" : "");
    }
    
    @Override public long getSimulatedSize() {
      // TODO Auto-generated method stub
      return 1;
    }
  }
}
