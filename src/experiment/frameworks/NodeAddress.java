package experiment.frameworks;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * A container for a specific framework node contains specific implementation of
 * a constructor and a compareTo operator for each node type
 * 
 * @author Alexander Libov
 * 
 */
public class NodeAddress implements Serializable, Comparable<NodeAddress> {
  /**
	 * 
	 */
  private static final long serialVersionUID = 39102314570705799L;
  public static final int SIZE = 200;
  
  @Override public int hashCode() {
    return name.hashCode();
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return toString().equals(obj.toString());
  }
  
  public final Object node;
  private final String name;
  
  public NodeAddress(final peersim.core.Node node2) {
    if (node2 == null) {
      throw new IllegalStateException("node shouldn't be null");
    }
    node = node2;
    name = String.valueOf(((peersim.core.Node) node).getID());
  }
  
  public NodeAddress(final InetSocketAddress node2, final String hostname) {
    if (node2 == null) {
      throw new IllegalStateException("node shouldn't be null");
    }
    node = node2;
    name = hostname + "-" + node2.getPort();
  }
  
  public String getName() {
    if (node instanceof peersim.core.Node) {
      return name;
    }
    if (node instanceof InetSocketAddress) {
      return name.substring(0, name.lastIndexOf("-"));
    }
    throw new IllegalStateException("illegal node type");
  }
  
  @Override public String toString() {
    return name;
    /* if (node instanceof InetSocketAddress) { final InetSocketAddress addr =
     * ((InetSocketAddress) node); if (addr.getAddress().isAnyLocalAddress()) {
     * try { return (InetAddress.getLocalHost().getHostName() + "-" +
     * addr.getPort()).toLowerCase(); } catch (final UnknownHostException e) {
     * System.err.println("unknown local host! using configuration name");
     * return (Common.currentConfiguration.name + "-" +
     * addr.getPort()).toLowerCase(); } } return
     * (addr.getAddress().getHostName() + "-" + addr.getPort()).toLowerCase(); }
     * return node.toString().toLowerCase(); */
  }
  
  @Override public int compareTo(final NodeAddress o) {
    return name.compareTo(o.name);
  }
}
