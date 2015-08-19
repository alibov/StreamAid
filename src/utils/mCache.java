package utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import experiment.frameworks.NodeAddress;

public class mCache {
  private final Queue<NodeAddress> queue;
  private final Integer maxSize;
  private final NodeAddress mCachesNode;
  private final Random r;
  
  public mCache(final Integer maxSize, final NodeAddress mCachesNode, final Random r) {
    if (maxSize == null) {
      throw new IllegalArgumentException("maxSize mustn't be null");
    }
    this.r = r;
    this.maxSize = maxSize;
    this.mCachesNode = mCachesNode;
    queue = new LinkedList<NodeAddress>();
  }
  
  public void insertCollectionOfNodes(final Collection<NodeAddress> collection) {
    queue.removeAll(collection);
    queue.addAll(collection);
    queue.remove(mCachesNode);
    final int toBeRemoved = queue.size() - maxSize;
    for (int i = 0; i < toBeRemoved; i++) {
      queue.poll();
    }
  }
  
  public void getRandomNodes(final Collection<NodeAddress> collection, final Integer num, final Collection<NodeAddress> except) {
    if (queue.isEmpty()) {
      return;
    }
    final Collection<NodeAddress> except_aux = new HashSet<NodeAddress>();
    except_aux.addAll(except);
    for (int i = 0; i < num; i++) {
      final NodeAddress pickedNode = Utils.pickRandomElementExcept(queue, except_aux, r);
      collection.add(pickedNode);
      except_aux.add(pickedNode);
      if (except_aux.size() == maxSize) {
        break;
      }
    }
  }
  
  public boolean contains(final NodeAddress node) {
    return queue.contains(node);
  }
  
  public Integer size() {
    return queue.size();
  }
  
  public void toCollection(final Collection<NodeAddress> collection) {
    collection.addAll(queue);
  }
  
  @Override public String toString() {
    if (queue.size() == 0) {
      return "empty mCache";
    }
    String str = new String();
    str += "[ ";
    for (final NodeAddress node : queue) {
      str += node.getName() + ", ";
    }
    str += " ]";
    return str;
  }
}