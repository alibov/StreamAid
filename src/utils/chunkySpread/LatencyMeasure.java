package utils.chunkySpread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import entites.VideoStreamChunk;

public class LatencyMeasure {
  private long currIndex = -1;
  private final int windowSize, treesNum, fastest, slowest;
  private final double fastPercentage, slowPercentage, highScore, lowScore;
  private final Map<Long, List<Integer>> arrivalOrders;
  
  public LatencyMeasure(final int _windowSize, final int _treesNum, final double _fastPercentage, final double _slowPercentage,
      final double _highScore, final double _lowScore) {
    windowSize = _windowSize;
    fastPercentage = _fastPercentage;
    slowPercentage = _slowPercentage;
    treesNum = _treesNum;
    highScore = _highScore;
    lowScore = _lowScore;
    fastest = (int) (fastPercentage * treesNum);
    slowest = (int) (slowPercentage * treesNum);
    arrivalOrders = new TreeMap<Long, List<Integer>>();
  }
  
  public void putChunk(final VideoStreamChunk chunk) {
    // if window should be moved
    if (chunk.index > currIndex) {
      currIndex = chunk.index;
      arrivalOrders.remove(currIndex - windowSize);
      arrivalOrders.put(currIndex, new ArrayList<Integer>());
    }
    if (arrivalOrders.keySet().contains(chunk.index)) {
      for (final int i : chunk.getDescriptions()) {
        if (!arrivalOrders.get(currIndex).contains(i)) {
          arrivalOrders.get(currIndex).add(i);
        }
      }
    }
  }
  
  public Map<Integer, LatencyState> treesLatencies() {
    final Map<Integer, Integer> latencyScores = new TreeMap<Integer, Integer>();
    for (int i = 0; i < treesNum; i++) {
      latencyScores.put(i, 0);
    }
    // count scores
    for (final long chunkIndex : arrivalOrders.keySet()) {
      final List<Integer> arrivalOrder = new ArrayList<Integer>(arrivalOrders.get(chunkIndex));
      final Set<Integer> laters = new TreeSet<Integer>();
      for (int i = 0; i < treesNum; i++) {
        if (!arrivalOrder.contains(i)) {
          laters.add(i);
        }
      }
      for (int i = 0; i < fastest && !arrivalOrder.isEmpty(); i++) {
        final int currTree = arrivalOrder.remove(0);
        latencyScores.put(currTree, latencyScores.get(currTree) + 1);
      }
      arrivalOrder.addAll(laters);
      for (int i = 0; i < slowest && !arrivalOrder.isEmpty(); i++) {
        final int currTree = arrivalOrder.remove(arrivalOrder.size() - 1);
        latencyScores.put(currTree, latencyScores.get(currTree) - 1);
      }
    }
    // build the summary map
    final Map<Integer, LatencyState> treesLatencies = new TreeMap<Integer, LatencyState>();
    for (final int tree : latencyScores.keySet()) {
      if (latencyScores.get(tree) >= highScore) {
        treesLatencies.put(tree, LatencyState.FAST);
      } else if (latencyScores.get(tree) <= lowScore) {
        treesLatencies.put(tree, LatencyState.SLOW);
      } else {
        treesLatencies.put(tree, LatencyState.MEDICORE);
      }
    }
    return treesLatencies;
  }
}
