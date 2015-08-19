package messages.chunkySpread;

import com.google.common.hash.BloomFilter;

import experiment.frameworks.NodeAddress;

public class AdoptRequest extends ParentSwapProtocol {
  /**
   * 
   */
  private static final long serialVersionUID = -8617457980212527258L;
  private final BloomFilter<NodeAddress> bloomFilter;
  
  public AdoptRequest(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final NodeAddress thirdParty, final BloomFilter<NodeAddress> bloomFilter, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID, treeID, thirdParty, _isLoadBalancing);
    this.bloomFilter = bloomFilter;
  }
  
  public BloomFilter<NodeAddress> getBloomFilter() {
    return bloomFilter;
  }
}
