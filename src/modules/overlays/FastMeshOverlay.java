package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import messages.FastMeshAcceptAncestorParentsAddSon;
import messages.FastMeshAcceptMessage;
import messages.FastMeshAcceptSendParentsMessage;
import messages.FastMeshConnectionRequestApprovedMessage;
import messages.FastMeshGetMaxDelayAndRB;
import messages.FastMeshGrantMessage;
import messages.FastMeshLeaveMessage;
import messages.FastMeshRemoveFromSonsMessage;
import messages.FastMeshRequestMessage;
import messages.FastMeshSendMaxDelayAndRB;
import messages.FastMeshSendToNeighborsMessage;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import modules.P2PClient;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class FastMeshOverlay extends MultipleTreeOverlayModule<Object> {
  /*---------Data members----------*/
  final int TTL_CONST;
  final int TEBA_CONST;
  final int TTWFG_CONST;
  protected long _maxDelayFromSource; // the delay of node. D in the article.
  protected NodeAddress _fatherWithMaxDelay;
  protected int _totalBandwidth; // the total upload bandwidth of node. U in the
  // article
  protected int _residualBandwidth = 0; // The residual bandwidth. R in the
  // article.
  protected int _numOfUnitsToPlay; // number of chunks to play. s in the
  // article.
  protected int _numOfUnitsReceived; // number of chunks received.
  // map new data to node
  protected Map<NodeAddress, Long> _nodesGranted = new HashMap<NodeAddress, Long>();
  protected Map<NodeAddress, Integer> _bandwidthTakenBySons = new HashMap<NodeAddress, Integer>();
  protected Map<NodeAddress, Integer> _bandwidthServedByFathers = new HashMap<NodeAddress, Integer>();
  protected Map<NodeAddress, Long> _knownNodesDelays = new HashMap<NodeAddress, Long>();
  protected Map<NodeAddress, Long> _chosenFathersDelays = new HashMap<NodeAddress, Long>();
  protected Map<NodeAddress, Integer> _knownNodesRB = new HashMap<NodeAddress, Integer>();
  protected Set<NodeAddress> _waitForResponseNodes = new HashSet<NodeAddress>();
  protected int _countGrantedRounds;
  private int _grantedFlag;
  protected int _numOfGrantedNodes;
  protected int _countGrantedNodes;
  private int _timeEpisodeBetweenAdaptation;
  private int _timeToWaitForGrant;
  protected boolean _fatherIsDown = false;
  protected boolean _allowAllConnectWithMe = false;
  protected NodeAddress[] _chunksArrServed; // a vector of nodes,
  
  // each index is the
  // parents providing
  // this chunk.
  /*------------------*/
  /**
   * evaluates the power of a node
   *
   * @param groupSize
   *          = The size of the group to choose fathers from.
   */
  public FastMeshOverlay(final P2PClient client, final int groupSize, final int TTL, final int TEBA, final int TTWFG, final Random r) {
    super(client, r);
    // get tracker
    addIngredient(new RandomGroupBootstrapIngredient(groupSize, new Random(r.nextLong())), client);
    TTL_CONST = TTL; // time-to-live, indicating the
    // number
    // of upstream levels REQUEST will go.
    TEBA_CONST = TEBA;
    TTWFG_CONST = TTWFG; // time to wait for grantes
    final int numDesc = Common.currentConfiguration.descriptions;
    _numOfUnitsToPlay = numDesc;
    /** the units are KB/sec divided by the size of a piece of a chunk */
    _totalBandwidth = (int) (network.getUploadBandwidth() / Common.currentConfiguration.bitRate) / numDesc;
    _residualBandwidth = _totalBandwidth;
    _numOfUnitsReceived = 0;
    _maxDelayFromSource = 0;
    _fatherWithMaxDelay = null;
    _grantedFlag = -1;
    _numOfGrantedNodes = 0;
    _countGrantedNodes = 0;
    _timeToWaitForGrant = 0;
    _timeEpisodeBetweenAdaptation = 0;
    _chunksArrServed = new NodeAddress[numDesc];
    for (int i = 0; i < numDesc; i++) {
      _chunksArrServed[i] = null;
    }
  }
  
  /* ------- General Help functions -------- */
  /** evaluates the power of a node */
  double evaluatePowerOfNode(final NodeAddress node1) {
    final double sRate = _knownNodesRB.get(node1) < _numOfUnitsToPlay ? _knownNodesRB.get(node1) : _numOfUnitsToPlay;
    final double delay = _knownNodesDelays.get(node1) + network.getEstimatedLatency(node1);
    return (sRate / delay);
  }
  
  protected void updateMaxDelay() {
    for (final NodeAddress father : getNodeGroup(fathersGroupName)) {
      _maxDelayFromSource = -1;
      final long tempDelay = _chosenFathersDelays.get(father) + network.getEstimatedLatency(father);
      if (_maxDelayFromSource < tempDelay) {
        _maxDelayFromSource = tempDelay;
        _fatherWithMaxDelay = father;
      }
    }
  }
  
  protected void updateFathersGroup() {
    /**
     * final Set<NodeAddress> randomSet = getNodeGroup(fathersGroupName, false);
     * final int sizer = randomSet.size(); if (sizer > 1) { int i = 0; i++; }
     */
    for (int i = 0; i < _numOfUnitsToPlay; ++i) {
      if (_chunksArrServed[i] != null && !network.isUp(_chunksArrServed[i])) {
        _numOfUnitsReceived--;
        _fatherIsDown = true;
        removeNeighbor(_chunksArrServed[i]);
        _bandwidthServedByFathers.remove(_chunksArrServed[i]);// not good
        _chunksArrServed[i] = null;
        _chosenFathersDelays.remove(_chunksArrServed[i]);
      }
    }
    if (_fatherWithMaxDelay != null && !network.isUp(_fatherWithMaxDelay)) {
      updateMaxDelay();
    }
  }
  
  protected void updateSonsGroup() {
    final Set<NodeAddress> tempKeySet = new HashSet<NodeAddress>(_bandwidthTakenBySons.keySet());
    for (final NodeAddress son : tempKeySet) {
      if (!network.isUp(son)) {
        removeNeighbor(son);
        _residualBandwidth += _bandwidthTakenBySons.get(son);
        _bandwidthTakenBySons.remove(son);
        for (final int descriptor : descriptorToSons.keySet()) {
          descriptorToSons.get(descriptor).remove(son);
        }
      }
    }
  }
  
  /* ----------------------------------- */
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      handleSeedNodeMultipleTargetsReplyMessage((SeedNodeMultipleTargetsReplyMessage) message);
    } else if (message instanceof FastMeshGetMaxDelayAndRB) {
      network.send(new FastMeshSendMaxDelayAndRB(getMessageTag(), network.getAddress(), message.sourceId, _maxDelayFromSource,
          _residualBandwidth));
    } else if (message instanceof FastMeshSendMaxDelayAndRB) {
      handleRecievedBandwidthAndDelay(message.sourceId, ((FastMeshSendMaxDelayAndRB) message).MRB,
          ((FastMeshSendMaxDelayAndRB) message).maxDelay, ((FastMeshSendMaxDelayAndRB) message).getIsReSend());
    } else if (message instanceof FastMeshRequestMessage) {
      handleRequestMessage((FastMeshRequestMessage) message);
    } else if (message instanceof FastMeshGrantMessage) {
      if (_grantedFlag == 1 || _grantedFlag == 2) {
        _nodesGranted.put(message.sourceId, ((FastMeshGrantMessage) message).maxDelay);
      }
    } else if (message instanceof FastMeshConnectionRequestApprovedMessage) {
      handleFastMeshConnectionRequestApprovedMessage((FastMeshConnectionRequestApprovedMessage) message);
    } else if (message instanceof FastMeshSendToNeighborsMessage) {
      for (final NodeAddress neigbour : getNodeGroup(fathersGroupName)) {
        if (network.isUp(neigbour) && neigbour != message.sourceId) {
          network.send(new FastMeshGetMaxDelayAndRB(getMessageTag(), message.sourceId, neigbour));
        }
      }
    } else if (message instanceof FastMeshAcceptMessage) {
      handleAcceptMassage((FastMeshAcceptMessage) message);
    } else if (message instanceof FastMeshAcceptSendParentsMessage) {
      handleAcceptParentsMessage((FastMeshAcceptSendParentsMessage) message);
    } else if (message instanceof FastMeshAcceptAncestorParentsAddSon) {
      handleAcceptAncestorParentsAddSon((FastMeshAcceptAncestorParentsAddSon) message);
    } else if (message instanceof FastMeshRemoveFromSonsMessage) {
      final NodeAddress son = message.sourceId;
      removeNeighbor(son);
      if (_bandwidthTakenBySons.containsKey(son)) {
        _residualBandwidth += _bandwidthTakenBySons.get(son);
        _bandwidthTakenBySons.remove(son);
        for (final int descriptor : descriptorToSons.keySet()) {
          descriptorToSons.get(descriptor).remove(son);
        }
      }
    } else if (message instanceof FastMeshLeaveMessage) {
      // do nothing
    } else {
      // throw new RuntimeException("We recieved un-handled message: " +
      // message);
    }
  }
  
  /** get fathers */
  protected void handleSeedNodeMultipleTargetsReplyMessage(final SeedNodeMultipleTargetsReplyMessage message) {
    for (final NodeAddress father : message.targets) {
      if (network.isUp(father)) {
        _waitForResponseNodes.add(father);
        _allowAllConnectWithMe = false;
        network.send(new FastMeshGetMaxDelayAndRB(getMessageTag(), network.getAddress(), father));
      }
    }
  }
  
  /**
   * handle data received from fathers, choose the best father when all the
   * potently fathers sent their data.
   */
  protected void handleRecievedBandwidthAndDelay(final NodeAddress sourceId, final int residualBandwith, final long delay,
      final boolean isReSent) {
    if (isReSent) {
      removeNeighbor(sourceId);
      for (int i = 0; i < _numOfUnitsToPlay; i++) {
        if (_chunksArrServed[i] == sourceId) {
          _numOfUnitsReceived--;
          _chunksArrServed[i] = null;
        }
      }
      if (_fatherWithMaxDelay == sourceId) {
        updateMaxDelay();
      }
    }
    _waitForResponseNodes.remove(sourceId);
    if (!network.isUp(sourceId)) {
      return;
    }
    // erase node already died
    final Set<NodeAddress> iterateSet = new HashSet<NodeAddress>(_knownNodesDelays.keySet());
    for (final NodeAddress father : iterateSet) {
      if (!network.isUp(father)) {
        _knownNodesDelays.remove(father);
        _knownNodesRB.remove(father);
      }
    }
    if (residualBandwith > 0) {
      _knownNodesDelays.put(sourceId, delay);
      _knownNodesRB.put(sourceId, residualBandwith);
    }
    if (_allowAllConnectWithMe || _waitForResponseNodes.isEmpty()) {
      final Set<NodeAddress> potentialFathers = new HashSet<NodeAddress>(_knownNodesDelays.keySet());
      potentialFathers.remove(network.getAddress());
      NodeAddress fatherChosen = null;
      double evaluationOfNode, maxEvaluation = -1;
      while (_numOfUnitsReceived < _numOfUnitsToPlay) {
        if (!potentialFathers.isEmpty()) {
          for (final NodeAddress father : potentialFathers) {
            if (network.isUp(father)) {
              if ((evaluationOfNode = evaluatePowerOfNode(father)) > maxEvaluation) {
                maxEvaluation = evaluationOfNode;
                fatherChosen = father;
              }
            } else {
              throw new RuntimeException("For some reason the node: " + sourceId.getName() + "died in the middle of doing stuff");
            }
          }
        } else {
          _allowAllConnectWithMe = true;
          for (final NodeAddress father : getNodeGroup(fathersGroupName)) {
            network.send(new FastMeshSendToNeighborsMessage(getMessageTag(), network.getAddress(), father));
          }
          return;
        }
        // We chose a father.
        addToGroup(fathersGroupName, fatherChosen);
        final Set<Integer> takenDescriptors = new HashSet<Integer>();
        // take data
        final int currentFatherBandwith = _knownNodesRB.get(fatherChosen);
        int takenBandwith = 0;
        for (int pieceTaken = 0; pieceTaken < _numOfUnitsToPlay; ++pieceTaken) {
          if (currentFatherBandwith - takenBandwith <= 0 || _numOfUnitsReceived == _numOfUnitsToPlay) {
            break;
          }
          if (_chunksArrServed[pieceTaken] != null) {
            continue;
          }
          _chunksArrServed[pieceTaken] = fatherChosen;
          takenBandwith++;
          _numOfUnitsReceived++;
          takenDescriptors.add(pieceTaken);
        }
        network.send(new FastMeshConnectionRequestApprovedMessage(getMessageTag(), network.getAddress(), fatherChosen,
            takenBandwith, takenDescriptors));
        if (_maxDelayFromSource < _knownNodesDelays.get(fatherChosen) + network.getEstimatedLatency(fatherChosen)) {
          _maxDelayFromSource = _knownNodesDelays.get(fatherChosen) + network.getEstimatedLatency(fatherChosen);
          _fatherWithMaxDelay = fatherChosen;
        }
        _chosenFathersDelays.put(fatherChosen, _knownNodesDelays.get(fatherChosen));
        potentialFathers.remove(fatherChosen);
        _knownNodesDelays.remove(fatherChosen);
        _knownNodesRB.remove(fatherChosen);
        fatherChosen = null;
        evaluationOfNode = maxEvaluation = -1;
      }
    }
  }
  
  /** father accept his new son */
  protected void handleFastMeshConnectionRequestApprovedMessage(final FastMeshConnectionRequestApprovedMessage message) {
    if ((_residualBandwidth - message.bandwidthTakenFromFather) < 0) {
      network.send(new FastMeshSendMaxDelayAndRB(getMessageTag(), network.getAddress(), message.sourceId, _maxDelayFromSource,
          _residualBandwidth, true));
      return;
    }
    addNeighbor(message.sourceId);
    addToGroup(sonsGroupName, message.sourceId);
    _bandwidthTakenBySons.put(message.sourceId, message.bandwidthTakenFromFather);
    _residualBandwidth -= message.bandwidthTakenFromFather;
    for (final int takenDescriptor : message.takenDescriptors) {
      if (descriptorToSons.get(takenDescriptor) != null) {
        descriptorToSons.get(takenDescriptor).add(message.sourceId);
      } else {
        final Set<NodeAddress> newNodeSet = new HashSet<NodeAddress>();
        newNodeSet.add(message.sourceId);
        descriptorToSons.put(takenDescriptor, newNodeSet);
      }
    }
  }
  
  /*-------------Handle the REQUEST GRANT ACCEPT mechanism-----------------*/
  /** Give the sender permission of taking over my position in the mesh. */
  protected void handleRequestMessage(final FastMeshRequestMessage message) {
    final int TTL = message.TTL - 1;
    if (TTL > 0) {
      for (final NodeAddress parent : getNodeGroup(fathersGroupName)) {
        network.send(new FastMeshRequestMessage(getMessageTag(), message.sourceId, parent, TTL, message.MTB));
      }
    }
    if (_totalBandwidth < message.MTB) {
      network.send(new FastMeshGrantMessage(getMessageTag(), network.getAddress(), message.sourceId, _maxDelayFromSource));
    }
  }
  
  /** Choose the father to take his position */
  protected void handleChooseAncestorAndReplace() {
    if (_residualBandwidth <= _numOfUnitsToPlay || _nodesGranted.size() == 0) {
      _grantedFlag = 0;
      return;
    }
    NodeAddress ancestorToReplace = null;
    long min = Integer.MAX_VALUE;
    long temp;
    for (final NodeAddress ancestor : _nodesGranted.keySet()) {
      if ((temp = _nodesGranted.get(ancestor)) < min) {
        min = temp;
        ancestorToReplace = ancestor;
      }
    }
    if (ancestorToReplace != null) {
      addToGroup(sonsGroupName, ancestorToReplace); // add the ancestor as a
      // son.
    } else {
      _grantedFlag = 0;
      return;// we don't have a valid grant.
    }
    // I supply all the ancestor stream
    _bandwidthTakenBySons.put(ancestorToReplace, _numOfUnitsToPlay);
    for (int desc = 0; desc < _numOfUnitsToPlay; ++desc) {
      if (descriptorToSons.get(desc) != null) {
        descriptorToSons.get(desc).add(ancestorToReplace);
      } else {
        final Set<NodeAddress> newNodeSet = new HashSet<NodeAddress>();
        newNodeSet.add(ancestorToReplace);
        descriptorToSons.put(desc, newNodeSet);
      }
    }
    _residualBandwidth -= _numOfUnitsToPlay;
    network.send(new FastMeshAcceptMessage(getMessageTag(), network.getAddress(), ancestorToReplace, _maxDelayFromSource));
  }
  
  protected void handleAcceptMassage(final FastMeshAcceptMessage message) {
    removeNeighbors(new HashSet<NodeAddress>(getNodeGroup(fathersGroupName)));
    addToGroup(fathersGroupName, message.sourceId);
    _fatherWithMaxDelay = message.sourceId;
    _chosenFathersDelays.put(message.sourceId, message._nodeDelay);
    // send your new father, the old fathers data so he can take it over.
    network.send(new FastMeshAcceptSendParentsMessage(getMessageTag(), network.getAddress(), message.sourceId,
        _bandwidthServedByFathers, _chunksArrServed, _chosenFathersDelays));
    _bandwidthServedByFathers.clear();
    _numOfUnitsReceived = 0;
    _bandwidthServedByFathers.put(message.sourceId, _numOfUnitsToPlay);
    for (int desc = 0; desc < _numOfUnitsToPlay; desc++) {
      _chunksArrServed[desc] = message.sourceId;
      _numOfUnitsReceived++;
    }
  }
  
  /** Node accept his new parents */
  protected void handleAcceptParentsMessage(final FastMeshAcceptSendParentsMessage message) {
    // tell me old parents - remove me
    for (final NodeAddress parent : getNodeGroup(fathersGroupName)) {
      network.send(new FastMeshRemoveFromSonsMessage(getMessageTag(), network.getAddress(), parent));
    }
    for (int i = 0; i < _numOfUnitsToPlay; i++) {
      _chunksArrServed[i] = null;
    }
    // add new parents (my old father parents)
    _chunksArrServed = message._fatherChunkArr.clone();
    getNodeGroup(fathersGroupName).clear();
    _bandwidthServedByFathers.clear();
    _bandwidthServedByFathers.putAll(message.parents);
    _chosenFathersDelays.clear();
    _chosenFathersDelays.putAll(message._fatherchosenFathersDelays);
    _fatherWithMaxDelay = null;
    boolean iAmMyFather = false;
    for (final NodeAddress parent : message.parents.keySet()) {
      if (!parent.equals(network.getAddress())) {
        addToGroup(fathersGroupName, parent);
        network.send(new FastMeshAcceptAncestorParentsAddSon(getMessageTag(), network.getAddress(), parent, message.sourceId));
      } else {
        iAmMyFather = true;
        _fatherIsDown = true;
      }
    }
    if (iAmMyFather) {
      for (int i = 0; i < _numOfUnitsToPlay; ++i) {
        if (_chunksArrServed[i] == network.getAddress()) {
          _chunksArrServed[i] = null;
        }
      }
      _bandwidthServedByFathers.remove(network.getAddress());
    }
    updateMaxDelay();
  }
  
  /** parents add their new son */
  protected void handleAcceptAncestorParentsAddSon(final FastMeshAcceptAncestorParentsAddSon message) {
    addToGroup(sonsGroupName, message.sourceId);
    removeNeighbor(message.ancestorToReplace);
    // removes the old son bandwidth.
    if (_bandwidthTakenBySons.containsKey(message.ancestorToReplace)) {
      final int bandwidthNewSon = _bandwidthTakenBySons.remove(message.ancestorToReplace);
      for (final int desc : descriptorToSons.keySet()) {
        if (descriptorToSons.get(desc).remove(message.ancestorToReplace)) {
          descriptorToSons.get(desc).add(message.sourceId);
        }
      }
      _bandwidthTakenBySons.put(message.sourceId, bandwidthNewSon);
    }
  }
  
  /*-------------------------------*/
  @Override public void nextCycle() {
    super.nextCycle();
    _timeEpisodeBetweenAdaptation++;
    updateMaxDelay();
    /*-- Check aliveness --*/
    // wait for response group
    final Set<NodeAddress> iterateSet = new HashSet<NodeAddress>(_waitForResponseNodes);
    for (final NodeAddress responseNode : iterateSet) {
      if (!network.isUp(responseNode)) {
        _waitForResponseNodes.remove(responseNode);
      }
    }
    // granted group
    for (final NodeAddress grantedNode : new HashSet<NodeAddress>(_nodesGranted.keySet())) {
      if (!network.isUp(grantedNode)) {
        _nodesGranted.remove(grantedNode);
      }
    }
    // fathers
    updateFathersGroup();
    // sons
    updateSonsGroup();
    /*-- handle REQUEST GRANT ACCEPT method.--*/
    /* _grantedFlag = 0 means we didn't start looking for replace. */
    /* _grantedFlag = 1 means we sent request messages and we waiting for GRANT
     * from father */
    /* _grantedFlag = 2 we finished waiting for GRANTS now we look for ancestor
     * to replace with */
    switch (_grantedFlag) {
      case 0:
        if (_residualBandwidth > _numOfUnitsToPlay && network.getServerNode().getName() != network.getAddress().getName()
            && _timeEpisodeBetweenAdaptation >= TEBA_CONST) {
          _timeEpisodeBetweenAdaptation = 0;
          _grantedFlag = 1;
          for (final NodeAddress parent : getNodeGroup(fathersGroupName)) {
            if (parent.getName() != network.getServerNode().getName()) {
              network.send(new FastMeshRequestMessage(getMessageTag(), network.getAddress(), parent, TTL_CONST, _totalBandwidth));
            }
          }
        }
        break;
      case 1:
        if (_nodesGranted.size() != 0) {
          _timeToWaitForGrant++;
          // if (_timeToWaitForGrant >= TTWFG_CONST)
          {
            _timeToWaitForGrant = 0;
            _grantedFlag = 2;
          }
        } else {
          _timeToWaitForGrant = 0;
          _grantedFlag = 0;
        }
        break;
      case 2:
        _grantedFlag = -1;
        handleChooseAncestorAndReplace();
        _nodesGranted.clear();
        break;
      case -1:
      default:
        _grantedFlag = 0;
        break;
    }
  }
  
  @Override public boolean isOverlayConnected() {
    if (_fatherIsDown) {// if a node lost his father lose connection.
      return false;
    }
    return super.isOverlayConnected();
  }
}
