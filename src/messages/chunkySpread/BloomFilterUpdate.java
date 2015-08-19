package messages.chunkySpread;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public class BloomFilterUpdate extends Bubbling {
  /**
   * 
   */
  private static final long serialVersionUID = 7726395670373872312L;
  
  public BloomFilterUpdate(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int _treeID,
      final BloomFilter<NodeAddress> _bloomFilter) {
    super(tag, sourceId, destID, _treeID, _bloomFilter);
  }
}
