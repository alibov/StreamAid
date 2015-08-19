package messages;

import experiment.frameworks.NodeAddress;

public class MembershipMessage extends Message {
  public final long seq_num;
  public final long time_to_live;
  public final int num_partner;
  public NodeAddress id;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE + NodeAddress.SIZE + Long.SIZE * 2;
  }
  
  public MembershipMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final long seq_num, final int num_partner, final long time_to_live, final NodeAddress id) {
    super(tag, sourceId, destID);
    this.seq_num = seq_num;
    this.num_partner = num_partner;
    this.time_to_live = time_to_live;
    this.id = id;
  }
  
  private static final long serialVersionUID = -7555691347452453472L;
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(id)) {
      id = newSource;
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return "seq:" + seq_num + " M:" + num_partner + " TTL" + time_to_live;
  }
}
