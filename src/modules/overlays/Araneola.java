package modules.overlays;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.AraneolaChangeConnectionMessage;
import messages.AraneolaConnectOKMessage;
import messages.AraneolaConnectToMessage;
import messages.AraneolaDisconnectMessage;
import messages.AraneolaDisconnectOKMessage;
import messages.AraneolaLeaveMessage;
import messages.AraneolaRedirectMessage;
import messages.ConnectionRequestMessage;
import messages.Message;
import modules.P2PClient;
import utils.Utils;
import entites.SizeableInt;
import experiment.frameworks.NodeAddress;

public class Araneola extends OverlayModule<Integer> {
  public final GossipingOverlay membershipView;
  public final int S; // membership view size
  public final int gossipDelay; // seconds between each gossip of the membership
  // view
  public final int amountToSend; // amount of membership information to send on
  // each gossiping round
  public final int L; // target number of neighbors
  public final int H; // upper bound on the number of neighbors
  public final int connect_timeout; // number of seconds between two consecutive
  // runs of the connect task
  public final int disconnect_timeout; // number of seconds between two
  // consecutive runs of the disconnect
  // task
  private int curr_connect_timeout = 0;
  private int curr_disconnect_timeout = 0;
  // data structures defined in the Araneola paper
  // private final Map<NodeSpecificImplementation, Integer> neighbors = new
  // TreeMap<NodeSpecificImplementation, Integer>();
  private final TreeMap<NodeAddress, Integer> next_round_connect = new TreeMap<NodeAddress, Integer>();
  private final Set<NodeAddress> connect_to_node = new TreeSet<NodeAddress>();
  private boolean rule2_flag = false;
  private final Set<NodeAddress> cands = new TreeSet<NodeAddress>();
  
  public Araneola(final P2PClient client, final int S, final int gossipDelay, final int amountToSend, final int L, final int H,
      final int connect_timeout, final int disconnect_timeout, final java.util.Random r) {
    super(client, r);
    membershipView = new GossipingOverlay(client, S, gossipDelay, amountToSend, new Random(r.nextLong()));
    this.S = S;
    this.gossipDelay = gossipDelay;
    this.amountToSend = amountToSend;
    this.L = L;
    this.H = H;
    this.connect_timeout = connect_timeout;
    this.disconnect_timeout = disconnect_timeout;
  }
  
  public Araneola(final P2PClient client, final Random r) {
    this(client, 12, 6, 3, 3, 10, 2, 2, r);
  }
  
  @Override public void setConfNumber(final int confNumber) {
    super.setConfNumber(confNumber);
    membershipView.setConfNumber(confNumber);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    membershipView.deactivate();
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ConnectionRequestMessage<?>) {
      if (!network.isUp(message.sourceId)) {
        return;
      }
      if (getNeighbors().size() + connect_to_node.size() < H || getNeighbors().contains(message.sourceId)) {
        add_connection(message.sourceId, (((SizeableInt) ((ConnectionRequestMessage<?>) message).payload).i));
      } else {
        final NodeAddress neighbor = Utils.findMinValueKey(getNeigborTable());
        network.send(new AraneolaRedirectMessage(getMessageTag(), network.getAddress(), message.sourceId, neighbor,
            getNeighborInfo(neighbor)));
      }
    } else if (message instanceof AraneolaRedirectMessage) {
      final AraneolaRedirectMessage arm = (AraneolaRedirectMessage) message;
      next_round_connect.put(arm.neighbor, arm.degree);
    } else if (message instanceof AraneolaConnectOKMessage) {
      if (getNeighbors().size() + connect_to_node.size() < H || connect_to_node.contains(message.sourceId)) {
        addNeighbor(message.sourceId, ((AraneolaConnectOKMessage) message).degree);
        if (connect_to_node.contains(message.sourceId)) {
          rule2_flag = false;
          connect_to_node.remove(message.sourceId);
        }
      } else {
        remove_connection(message.sourceId);
        network.send(new AraneolaLeaveMessage(getMessageTag(), network.getAddress(), message.sourceId));
      }
    } else if (message instanceof AraneolaLeaveMessage) {
      if (remove_connection(message.sourceId)) {
        network.send(new AraneolaLeaveMessage(getMessageTag(), network.getAddress(), message.sourceId));
      }
    } else if (message instanceof AraneolaDisconnectMessage) {
      if (getNeighbors().size() > L || cands.contains(message.sourceId)) {
        remove_connection(message.sourceId);
        if (network.isUp(message.sourceId)) {
          network.send(new AraneolaDisconnectOKMessage(getMessageTag(), network.getAddress(), message.sourceId));
        }
      }
      if (cands.contains(message.sourceId)) {
        rule2_flag = false;
      }
    } else if (message instanceof AraneolaDisconnectOKMessage) {
      remove_connection(message.sourceId);
    } else if (message instanceof AraneolaConnectToMessage) {
      final AraneolaConnectToMessage acm = (AraneolaConnectToMessage) message;
      if (getNeighbors().size() > L && !rule2_flag) {
        rule2_flag = true;
        connect_to_node.clear(); // TODO should it be cleared?
        connect_to_node.add(acm.h);
        network.send(new AraneolaChangeConnectionMessage(getMessageTag(), network.getAddress(), acm.h, getNeighbors().size(),
            acm.sourceId));
      }
    } else if (message instanceof AraneolaChangeConnectionMessage) {
      final AraneolaChangeConnectionMessage accm = (AraneolaChangeConnectionMessage) message;
      if (getNeighbors().size() < H && !rule2_flag) {
        rule2_flag = true;
        add_connection(accm.sourceId, accm.degree);
        if (getNeighbors().size() > L) {
          network.send(new AraneolaDisconnectMessage(getMessageTag(), network.getAddress(), accm.n));
        }
        rule2_flag = false;
      }
    }
  }
  
  private boolean remove_connection(final NodeAddress sourceId) {
    if (getNeighbors().size() < L) {
      curr_connect_timeout = 0;// wake up connect task
    }
    return removeNeighbor(sourceId);
  }
  
  private void add_connection(final NodeAddress sourceId, final int degree) {
    addNeighbor(sourceId, degree);
    network.send(new AraneolaConnectOKMessage(getMessageTag(), network.getAddress(), sourceId, getNeighbors().size()));
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    membershipView.nextCycle();
    if (!membershipView.isOverlayConnected()) {
      return;
    }
    connectTask();
    disconnectTask();
  }
  
  private void disconnectTask() {
    if (curr_disconnect_timeout > 0) {
      curr_disconnect_timeout--;
      return;
    }
    curr_disconnect_timeout = disconnect_timeout;
    final int i = getNeighbors().size() - L;
    if (i > 0) {
      /* Rule 1 */
      for (final Entry<NodeAddress, Integer> entry : getNeigborTable().entrySet()) {
        if (entry.getValue() == null) {
          continue;
        }
        if (entry.getValue() > L) {
          cands.add(entry.getKey());
        }
      }
      // leave with i candidates with lowest identifiers
      while (cands.size() > i) {
        cands.remove(Collections.max(cands));
      }
      for (final NodeAddress nsi : cands) {
        if (nsi.compareTo(network.getAddress()) < 0 && network.isUp(nsi)) {
          network.send(new AraneolaDisconnectMessage(getMessageTag(), network.getAddress(), nsi));
        }
      }
      if (cands.isEmpty() && !rule2_flag) {
        rule2_flag = true;
        final NodeAddress h = Utils.pickRandomElement(Utils.findMaxValueKeyGroup(getNeigborTable()), r);
        final NodeAddress l = Utils.pickRandomElement(Utils.findMinValueKeyGroup(getNeigborTable()), r);
        final int lDegree = getNeighborInfo(l);
        if (getNeighbors().size() >= lDegree + 2) {
          cands.add(h);
          network.send(new AraneolaConnectToMessage(getMessageTag(), network.getAddress(), l, h));
        }
      }
    }
  }
  
  private void connectTask() {
    if (curr_connect_timeout > 0) {
      curr_connect_timeout--;
      return;
    }
    curr_connect_timeout = connect_timeout;
    final int gap = L - getNeighbors().size();
    NodeAddress n = null;
    final Set<NodeAddress> view = new TreeSet<NodeAddress>(membershipView.getNeighbors());
    for (int i = 0; i < gap && !view.isEmpty(); i++) {
      while (!next_round_connect.isEmpty()) {
        n = next_round_connect.firstKey();
        next_round_connect.remove(n);
        if (network.isUp(n)) {
          break;
        }
        n = null;
      }
      if (n == null) {
        n = Utils.pickRandomElement(view, r);
      }
      network.send(new ConnectionRequestMessage<SizeableInt>(getMessageTag(), network.getAddress(), n, new SizeableInt(
          getNeighbors().size())));
      view.remove(n);
    }
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    membershipView.setServerMode();
  }
  
  @Override public void reConnect() {
    // Do nothing.
  }
}
