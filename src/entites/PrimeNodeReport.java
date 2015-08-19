package entites;

import interfaces.Sizeable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.Common;

public class PrimeNodeReport extends TreeMap<Long, TreeSet<Integer>> implements Sizeable {
  public Long playTime = Long.valueOf(-1);
  
  @Override public long getSimulatedSize() {
    long retVal = 0;
    for (final java.util.Map.Entry<Long, TreeSet<Integer>> entry : entrySet()) {
      retVal += Long.SIZE;
      retVal += Integer.SIZE * entry.getValue().size();
    }
    return retVal;
  }
  
  private static final long serialVersionUID = 1902177321702535987L;
  
  public PrimeNodeReport(final PrimeNodeReport requestToSend) {
    for (final java.util.Map.Entry<Long, TreeSet<Integer>> entry : requestToSend.entrySet()) {
      put(entry.getKey(), new TreeSet<Integer>(entry.getValue()));
    }
    playTime = requestToSend.playTime;
  }
  
  public PrimeNodeReport() {
    super();
  }
  
  @Override public String toString() {
    String output = "";
    if (playTime != -1) {
      output += "Node play time: " + playTime;
    }
    return output + super.toString();
  }
  
  /**
   * This function updates the mapping between time stamps and available
   * descriptors by adding the new report to the current records.
   * 
   * @param newReport
   *          - The new reports that given.
   */
  public void update(final PrimeNodeReport newReport) {
    for (final Long ts : newReport.keySet()) {
      if (!containsKey(ts)) {
        put(ts, newReport.get(ts));
      } else {
        //        final TreeSet<Integer> updateData = get(ts);
        //        updateData.addAll(newReport.get(ts));
        //        put(ts, updateData);
        get(ts).addAll(newReport.get(ts));
      }
    }
  }
  
  /**
   * usage outbox.getDelta(all available).
   * 
   * @param availableDescriptions
   * @return all descriptions that "this" PrimeNodeReport don't have and
   *         availableDescriptions do have.
   */
  public PrimeNodeReport getDelta(final PrimeNodeReport availableDescriptions) {
    final PrimeNodeReport $ = new PrimeNodeReport();
    for (final Long ts : availableDescriptions.keySet()) {
      if (!containsKey(ts)) {
        $.put(ts, availableDescriptions.get(ts));
      } else {
        final TreeSet<Integer> newData = new TreeSet<Integer>();
        final TreeSet<Integer> alreadyReported = get(ts);
        boolean hasNewDescriptions = false;
        for (final Integer i : availableDescriptions.get(ts)) {
          if (!alreadyReported.contains(i)) {
            newData.add(i);
            hasNewDescriptions = true;
          }
        }
        if (hasNewDescriptions) {
          $.put(ts, newData);
        }
      }
    }
    return $;
  }
  
  public PrimeNodeReport getIntersect(final PrimeNodeReport availableDescriptors) {
    final PrimeNodeReport $ = new PrimeNodeReport();
    for (final Long ts : availableDescriptors.keySet()) {
      if (!containsKey(ts)) {
        continue;
      }
      final TreeSet<Integer> newData = PrimeNodeReport.intersection(get(ts), availableDescriptors.get(ts));
      if (newData.size() > 0) {
        $.put(ts, newData);
      }
    }
    return $;
  }
  
  public static TreeSet<Integer> intersection(final TreeSet<Integer> setA, final TreeSet<Integer> setB) {
    final TreeSet<Integer> tmp = new TreeSet<Integer>();
    for (final Integer x : setA) {
      if (setB.contains(x)) {
        tmp.add(x);
      }
    }
    return tmp;
  }
  
  public Long getLatestTimestamp() {
    if (keySet().isEmpty()) {
      return (long) 0;
    }
    return Collections.max(keySet());
  }
  
  /**
   * 
   * @param prevMaxTimestamp
   * @param currMaxTimestamp
   * @return PrimeNodeReport which contains one random description that is
   *         available at "this" PrimeNodeReport per each timestamp in the given
   *         range.
   */
  public PrimeNodeReport getRandomSubset(final Long prevMaxTimestamp, final Long currMaxTimestamp, final Random r) {
    final PrimeNodeReport $ = new PrimeNodeReport();
    for (Long i = prevMaxTimestamp + 1; i <= currMaxTimestamp; i++) {
      if (containsKey(i)) {
        final List<Integer> indexes = new ArrayList<Integer>();
        final TreeSet<Integer> available = get(i);
        for (final Integer j : available) {
          indexes.add(j);
        }
        Collections.shuffle(indexes, r);
        final TreeSet<Integer> ts = new TreeSet<Integer>();
        ts.add(indexes.get(0));
        $.put(i, ts);
      }
    }
    return $;
  }
  
  public static TreeSet<Integer> getDifferenceSet(final TreeSet<Integer> set) {
    final TreeSet<Integer> $ = new TreeSet<Integer>();
    for (Integer i = 0; i < Common.currentConfiguration.descriptions; i++) {
      if (!set.contains(i)) {
        $.add(i);
      }
    }
    return $;
  }
}
