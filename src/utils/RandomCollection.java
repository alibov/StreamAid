package utils;

import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class RandomCollection<E> {
  private final NavigableMap<Double, E> map = new TreeMap<Double, E>();
  private final Random random;
  private double total = 0;
  
  public RandomCollection(final Random random) {
    this.random = random;
  }
  
  public void add(final double weight, final E result) {
    if (weight <= 0) {
      return;
    }
    total += weight;
    map.put(total, result);
  }
  
  public E next() {
    final double value = random.nextDouble() * total;
    return map.ceilingEntry(value).getValue();
  }
}