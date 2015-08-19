package messages;

import experiment.frameworks.NodeAddress;

public class KnownNodesInClubRequest extends EmptyMessage {
  /**
   * 
   */
  private static final long serialVersionUID = 5213060099121171027L;
  
  public KnownNodesInClubRequest(final String messageTag, final NodeAddress impl, final NodeAddress n) {
    super(messageTag, impl, n);
  }
}
