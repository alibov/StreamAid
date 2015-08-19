package messages;

import ingredients.overlay.InformationExchange.ExchangeType;
import ingredients.overlay.InformationExchange.InfoType;

import java.util.Map;

import experiment.frameworks.NodeAddress;

public class InfoRequestMessage extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 1917785343323844393L;
  public final Map<InfoType, ExchangeType> requestingInfoExchangeType;
  
  public InfoRequestMessage(final String messageTag, final NodeAddress src, final NodeAddress dst,
      final Map<InfoType, ExchangeType> requestingInfoExchangeType) {
    super(messageTag, src, dst);
    this.requestingInfoExchangeType = requestingInfoExchangeType;
  }
  
  @Override protected String getContents() {
    return requestingInfoExchangeType.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + requestingInfoExchangeType.size() * (Integer.SIZE * 2);
  }
}
