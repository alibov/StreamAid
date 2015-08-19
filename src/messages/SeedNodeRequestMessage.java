package messages;

import experiment.frameworks.NodeAddress;

public class SeedNodeRequestMessage extends EmptyMessage {
  private static final long serialVersionUID = -8537062503810264128L;
  
  public SeedNodeRequestMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
