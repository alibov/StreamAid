package ingredients.overlay;

import messages.EmptyMessage;
import experiment.frameworks.NodeAddress;

public class BTLivePing extends EmptyMessage {
  private static final long serialVersionUID = 7434952716994867164L;
  
  public BTLivePing(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
