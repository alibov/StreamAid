package experimentTests;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import utils.chunkySpread.LatencyMeasure;
import utils.chunkySpread.LatencyState;
import entites.MDCVideoStreamChunk;

public class LatencyMeasureTest {
  final int treesNum = 10;
  final int windowSize = 5;
  
  @Test public void basic() {
    final LatencyMeasure lm = new LatencyMeasure(windowSize, treesNum, 0.2, 0.2, 4, -4);
    for (long index = 0; index < windowSize; index++) {
      for (int tree = 0; tree < treesNum; tree++) {
        lm.putChunk(new MDCVideoStreamChunk(index, new TreeSet<Integer>(Collections.singleton(tree))));
      }
    }
    final Map<Integer, LatencyState> res = lm.treesLatencies();
    final Set<Integer> fast = new TreeSet<Integer>();
    final Set<Integer> slow = new TreeSet<Integer>();
    fast.add(0);
    fast.add(1);
    slow.add(8);
    slow.add(9);
    assertResults(treesNum, res, fast, slow);
  }
  
  @Test public void window() {
    final LatencyMeasure lm = new LatencyMeasure(windowSize, treesNum, 0.2, 0.2, 4, -4);
    for (long index = 0; index < windowSize; index++) {
      for (int tree = 0; tree < treesNum; tree++) {
        lm.putChunk(new MDCVideoStreamChunk(index, new TreeSet<Integer>(Collections.singleton(tree))));
      }
    }
    for (long index = windowSize; index < 2 * windowSize; index++) {
      for (int tree = treesNum - 1; tree >= 0; tree--) {
        lm.putChunk(new MDCVideoStreamChunk(index, new TreeSet<Integer>(Collections.singleton(tree))));
      }
    }
    final Map<Integer, LatencyState> res = lm.treesLatencies();
    final Set<Integer> fast = new TreeSet<Integer>();
    final Set<Integer> slow = new TreeSet<Integer>();
    fast.add(8);
    fast.add(9);
    slow.add(0);
    slow.add(1);
    assertResults(treesNum, res, fast, slow);
  }
  
  @Test public void comingLate() {
    final LatencyMeasure lm = new LatencyMeasure(windowSize, treesNum, 0.2, 0.2, 4, -4);
    for (long index = 0; index < 2 * windowSize; index++) {
      for (int tree = 0; tree < treesNum; tree++) {
        lm.putChunk(new MDCVideoStreamChunk(index, new TreeSet<Integer>(Collections.singleton(tree))));
      }
    }
    lm.putChunk(new MDCVideoStreamChunk(0, new TreeSet<Integer>(Collections.singleton(6))));
    lm.putChunk(new MDCVideoStreamChunk(1, new TreeSet<Integer>(Collections.singleton(6))));
    lm.putChunk(new MDCVideoStreamChunk(2, new TreeSet<Integer>(Collections.singleton(6))));
    lm.putChunk(new MDCVideoStreamChunk(3, new TreeSet<Integer>(Collections.singleton(6))));
    final Map<Integer, LatencyState> res = lm.treesLatencies();
    final Set<Integer> fast = new TreeSet<Integer>();
    final Set<Integer> slow = new TreeSet<Integer>();
    fast.add(0);
    fast.add(1);
    slow.add(8);
    slow.add(9);
    assertResults(treesNum, res, fast, slow);
  }
  
  @Test public void incompleteChunks() {
    final LatencyMeasure lm = new LatencyMeasure(windowSize, treesNum, 0.2, 0.2, 4, -4);
    for (long index = 0; index < windowSize; index++) {
      for (int tree = 0; tree < treesNum; tree++) {
        if (index != 3 && tree == 1) {
          continue;
        }
        lm.putChunk(new MDCVideoStreamChunk(index, new TreeSet<Integer>(Collections.singleton(tree))));
      }
    }
    final Map<Integer, LatencyState> res = lm.treesLatencies();
    System.out.println(res);
    final Set<Integer> fast = new TreeSet<Integer>();
    final Set<Integer> slow = new TreeSet<Integer>();
    fast.add(0);
    fast.add(2);
    slow.add(9);
    assertResults(treesNum, res, fast, slow);
  }
  
  private static void assertResults(final int treesNum, final Map<Integer, LatencyState> res, final Set<Integer> fast,
      final Set<Integer> slow) {
    // System.out.println("fast: " + fast);
    // System.out.println("res: " + res);
    for (int i = 0; i < treesNum; i++) {
      // System.out.println("Asserting " + i);
      if (fast.contains(i)) {
        Assert.assertEquals(LatencyState.FAST, res.get(i));
      } else if (slow.contains(i)) {
        Assert.assertEquals(LatencyState.SLOW, res.get(i));
      } else {
        Assert.assertEquals(LatencyState.MEDICORE, res.get(i));
      }
    }
  }
}
