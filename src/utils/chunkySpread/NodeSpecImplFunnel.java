package utils.chunkySpread;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import experiment.frameworks.NodeAddress;

public class NodeSpecImplFunnel implements Funnel<NodeAddress> {
  @Override public void funnel(final NodeAddress node, final PrimitiveSink into) {
    into.putInt(node.hashCode());
  }
}
