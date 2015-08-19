package messages;

import experiment.frameworks.NodeAddress;

public class AscendToTreeBoneMessage extends EmptyMessage {
  public AscendToTreeBoneMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  private static final long serialVersionUID = -3967014767357416049L;
}
