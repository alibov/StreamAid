package messages.chunkySpread;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public class AdoptRequestAccept extends ParentSwapProtocol {
  /**
   * 
   */
  private static final long serialVersionUID = -1010041434770485978L;
  private final BloomFilter<NodeAddress> bloomFilter;
  
  public AdoptRequestAccept(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final NodeAddress thirdParty, final BloomFilter<NodeAddress> bloomFilter, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID, treeID, thirdParty, _isLoadBalancing);
    this.bloomFilter = bloomFilter;
  }
  
  public BloomFilter<NodeAddress> getBloomFilter() {
    return bloomFilter;
  }
}
