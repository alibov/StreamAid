package messages.chunkySpread;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public class FloodTree extends Bubbling {
  /**
   * 
   */
  private static final long serialVersionUID = 2025298087179572049L;
  
  public FloodTree(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int _treeID,
      final BloomFilter<NodeAddress> _bloomFilter) {
    super(tag, sourceId, destID, _treeID, _bloomFilter);
  }
}