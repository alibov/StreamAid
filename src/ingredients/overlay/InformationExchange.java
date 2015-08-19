package ingredients.overlay;

import ingredients.AbstractIngredient;
import interfaces.InfoGetter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.InfoMessage;
import messages.InfoRequestMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class InformationExchange extends AbstractIngredient<OverlayModule<?>> {
  public InformationExchange() {
    super(null);
  }
  
  public enum InfoType {
    UPLOADBANDWIDTHPERNEIGHBOR
  }
  
  public enum ExchangeType {
    ONCONNECT, EVERYCYCLE, ONUPDATE
  }
  
  static Map<InfoType, InfoGetter> infoGetters = new HashMap<InformationExchange.InfoType, InfoGetter>();
  static {
    infoGetters.put(InfoType.UPLOADBANDWIDTHPERNEIGHBOR, new InfoGetter() {
      @Override public Object getInfo(final P2PClient client, final OverlayModule<?> overlay) {
        return client.network.getUploadBandwidth() / (overlay.getNeighbors().size() == 0 ? 1 : overlay.getNeighbors().size());
      }
    });
  }
  private final Map<InfoType, ExchangeType> requestedInfo = new HashMap<InformationExchange.InfoType, InformationExchange.ExchangeType>();
  private final Map<NodeAddress, Map<InfoType, ExchangeType>> requestedInfoSent = new HashMap<NodeAddress, Map<InfoType, ExchangeType>>();
  private final Map<NodeAddress, Map<InfoType, ExchangeType>> providingInfo = new HashMap<NodeAddress, Map<InfoType, ExchangeType>>();
  private final Map<NodeAddress, Map<InfoType, Object>> infoSent = new HashMap<NodeAddress, Map<InfoType, Object>>();
  private final Map<NodeAddress, Map<InfoType, Object>> infoReceived = new HashMap<NodeAddress, Map<InfoType, Object>>();
  
  public Object getInfo(final NodeAddress node, final InfoType info) {
    if (!infoReceived.containsKey(node)) {
      return null;
    }
    return infoReceived.get(node).get(info);
  }
  
  public void requestInfoExchange(final InfoType infoType, final ExchangeType exchangeType) {
    requestedInfo.put(infoType, exchangeType);
    // requestInfoFromNodes();
  }
  
  private void requestInfoFromNodes() {
    for (final NodeAddress na : alg.getNeighbors()) {
      if (requestedInfoSent.containsKey(na) && requestedInfoSent.get(na).equals(requestedInfo)) {
        break; // TODO maybe check info received instead?
      }
      client.network.send(new InfoRequestMessage(getMessageTag(), client.network.getAddress(), na, requestedInfo));
      requestedInfoSent.put(na, requestedInfo);
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    infoReceived.keySet().retainAll(alg.getNeighbors());
    requestInfoFromNodes();
    provideInfoToNodes();
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof InfoRequestMessage) {
      providingInfo.put(message.sourceId, ((InfoRequestMessage) message).requestingInfoExchangeType);
      provideInfoToNodes();
    } else if (message instanceof InfoMessage) {
      Utils.checkExistence(infoReceived, message.sourceId, new HashMap<InfoType, Object>());
      infoReceived.get(message.sourceId).putAll(((InfoMessage) message).info);
    }
  }
  
  private void provideInfoToNodes() {
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress node : providingInfo.keySet()) {
      if (!client.network.isUp(node)) {
        toRemove.add(node);
        continue;
      }
      final Map<InfoType, Object> infoToSend = new HashMap<InformationExchange.InfoType, Object>();
      final Map<InfoType, ExchangeType> reqInfo = providingInfo.get(node);
      for (final InfoType type : reqInfo.keySet()) {
        Utils.checkExistence(infoSent, node, new HashMap<InfoType, Object>());
        if (!infoSent.get(node).containsKey(type)
            || reqInfo.get(type).equals(ExchangeType.EVERYCYCLE)
            || (reqInfo.get(type).equals(ExchangeType.ONUPDATE) && !infoGetters.get(type).getInfo(client, alg)
                .equals(infoSent.get(node).get(type)))) {
          infoToSend.put(type, infoGetters.get(type).getInfo(client, alg));
        }
      }
      if (!infoToSend.isEmpty()) {
        client.network.send(new InfoMessage(getMessageTag(), client.network.getAddress(), node, infoToSend));
        infoSent.get(node).putAll(infoToSend);
      }
    }
    providingInfo.keySet().removeAll(toRemove);
    infoSent.keySet().removeAll(toRemove);
  }
}
