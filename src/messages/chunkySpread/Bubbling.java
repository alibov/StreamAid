package messages.chunkySpread;

import messages.Message;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public abstract class Bubbling extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = -8641715181820668161L;
  private final int treeID;
  private final BloomFilter<NodeAddress> bloomFilter;
  
  public Bubbling(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int _treeID,
      final BloomFilter<NodeAddress> _bloomFilter) {
    super(tag, sourceId, destID);
    treeID = _treeID;
    bloomFilter = _bloomFilter;
  }
  
  public int getTreeID() {
    return treeID;
  }
  
  public BloomFilter<NodeAddress> getBloomFilter() {
    return bloomFilter;
  }
  
  @Override protected String getContents() {
    return "treeID is " + treeID;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}