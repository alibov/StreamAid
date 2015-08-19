package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import messages.Message;

/**
 * Smaller utilities functions
 * 
 * @author Alexander Libov
 * 
 */
public class Utils {
  public interface Timeable {
    long getTime();
  }
  
  public static Timeable time;
  static public long movieStartTime = -1;
  
  static public void init() {
    movieStartTime = -1;
  }
  
  /**
   * 
   * @param map
   *          the map to be sorted
   * @return a new map sorted descending by value
   */
  public static <K extends Comparable<K>, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(final Map<K, V> map) {
    return sortByValue(map, false);
  }
  
  public static long getTime() {
    return time.getTime();
  }
  
  public static <K extends Comparable<K>, V extends Comparable<V>> K findMinValueKey(final Map<K, V> map) {
    if (map == null || map.isEmpty()) {
      throw new RuntimeException("no items in map or no map");
    }
    final Entry<K, V> firstEntry = map.entrySet().iterator().next();
    V minv = firstEntry.getValue();
    K mink = firstEntry.getKey();
    for (final Entry<K, V> entry : map.entrySet()) {
      if (entry.getValue().compareTo(minv) < 0) {
        minv = entry.getValue();
        mink = entry.getKey();
      }
    }
    return mink;
  }
  
  public static <K extends Comparable<K>, V extends Comparable<V>> Set<K> findMinValueKeyGroup(final Map<K, V> map) {
    if (map == null || map.isEmpty()) {
      throw new RuntimeException("no items in map or no map");
    }
    V minv = map.values().iterator().next();
    final Set<K> mink = new HashSet<K>();
    for (final Entry<K, V> entry : map.entrySet()) {
      if (entry.getValue().compareTo(minv) < 0) {
        minv = entry.getValue();
        mink.clear();
        mink.add(entry.getKey());
      } else if (entry.getValue().compareTo(minv) == 0) {
        mink.add(entry.getKey());
      }
    }
    return mink;
  }
  
  public static <K extends Comparable<K>, V extends Comparable<V>> Set<K> findMaxValueKeyGroup(final Map<K, V> map) {
    if (map == null || map.isEmpty()) {
      throw new RuntimeException("no items in map or no map");
    }
    V maxv = map.values().iterator().next();
    final Set<K> maxk = new HashSet<K>();
    for (final Entry<K, V> entry : map.entrySet()) {
      if (entry.getValue().compareTo(maxv) > 0) {
        maxv = entry.getValue();
        maxk.clear();
        maxk.add(entry.getKey());
      } else if (entry.getValue().compareTo(maxv) == 0) {
        maxk.add(entry.getKey());
      }
    }
    return maxk;
  }
  
  public static <K extends Comparable<K>, V extends Comparable<V>> LinkedHashMap<K, V> sortByValue(final Map<K, V> map,
      final boolean ascending) {
    final List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
      @Override public int compare(final Map.Entry<K, V> o1, final Map.Entry<K, V> o2) {
        int result = ((Comparable<V>) (o1).getValue()).compareTo((o2).getValue());
        if (result != 0) {
          return (ascending) ? result : -result;
        }
        result = ((Comparable<K>) (o1).getKey()).compareTo((o2).getKey());
        return (ascending) ? result : -result;
      }
    });
    final LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
    for (final Entry<K, V> entry : list) {
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }
  
  public static long getMovieTime() {
    if (movieStartTime == -1) {
      return -1;
    }
    return getTime() - movieStartTime;
  }
  
  public static <T> T pickRandomElement(final Collection<T> col, final Random r) {
    final int size = col.size();
    final int item = r.nextInt(size);
    int i = 0;
    for (final T obj : col) {
      if (i == item) {
        return obj;
      }
      i++;
    }
    throw new RuntimeException("Empty collection sent to pickRandomElement");
  }
  
  public static <K, V> void checkExistence(final Map<K, V> map, final K key, final V initValue) {
    if (!map.containsKey(key)) {
      map.put(key, initValue);
    }
  }
  
  public static <T> T pickRandomElementExcept(final Collection<T> col, final T except, final Random r) {
    final Collection<T> rest = new LinkedList<T>(col);
    rest.remove(except);
    return pickRandomElement(rest, r);
  }
  
  public static <T> T pickRandomElementExcept(final Collection<T> col, final Collection<T> exceptCol, final Random r) {
    final Collection<T> rest = new LinkedList<T>(col);
    rest.removeAll(exceptCol);
    return pickRandomElement(rest, r);
  }
  
  /* public static int getSize(final Serializable obj) { if (obj instanceof
   * ChunksMessage) { final ChunksMessage chm = (ChunksMessage) obj; if
   * (chm.chunks.isEmpty()) { return 0; } // TODO for simulation purposes return
   * chm.chunks.size() * Common.currentConfiguration.bitRate; } else if (obj
   * instanceof ChunkMessage) { final ChunkMessage chm = (ChunkMessage) obj; if
   * (chm.chunk.content == null) { return 0; } // TODO for simulation purposes
   * return Common.currentConfiguration.bitRate; } return
   * serializeObject(obj).length * 8; } */
  public static long getSize(final Message m) {
    return m.getSimulatedSize();
  }
  
  public static byte[] serializeObject(final Serializable obj) {
    try {
      final ByteArrayOutputStream bStream = new ByteArrayOutputStream();
      final ObjectOutput oo = new ObjectOutputStream(bStream);
      oo.writeObject(obj);
      oo.close();
      final byte[] serializedMessage = bStream.toByteArray();
      return serializedMessage;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Object deserializeObject(final byte[] obj) {
    ObjectInputStream oo = null;
    try {
      final ByteArrayInputStream bStream = new ByteArrayInputStream(obj);
      oo = new ObjectInputStream(bStream);
      final Object retVal = oo.readObject();
      return retVal;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        if (oo != null) {
          oo.close();
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  /**
   * By default File#delete fails for non-empty directories, it works like "rm".
   * We need something a little more brutal - this does the equivalent of
   * "rm -r"
   * 
   * @param path
   *          Root File Path
   * @return true iff the file and all sub files/directories have been removed
   * @throws FileNotFoundException
   */
  public static boolean deleteRecursive(final File path) throws FileNotFoundException {
    if (!path.exists()) {
      throw new FileNotFoundException(path.getAbsolutePath());
    }
    boolean ret = true;
    if (path.isDirectory()) {
      for (final File f : path.listFiles()) {
        ret = ret && deleteRecursive(f);
      }
    }
    return ret && path.delete();
  }
  
  public static String convertStreamToString(final java.io.InputStream is) {
    final java.util.Scanner s = new java.util.Scanner(is);
    s.useDelimiter("\\A");
    final String retVal = s.hasNext() ? s.next() : "";
    s.close();
    return retVal;
  }
  
  public static String getLocalHostName() {
    try {
      return convertStreamToString(Runtime.getRuntime().exec("hostname").getInputStream()).toLowerCase().trim();
    } catch (final IOException e) {
      try {
        return InetAddress.getLocalHost().getHostName().toLowerCase().trim();
      } catch (final UnknownHostException e1) {
        System.err.println("unknown local host! using configuration name");
        return Common.currentConfiguration.name.toLowerCase().trim();
      }
    }
  }
  
  public static String readFile(final String path) throws IOException {
    final BufferedReader br = new BufferedReader(new FileReader(path));
    try {
      final StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while (line != null) {
        sb.append(line);
        sb.append('\n');
        line = br.readLine();
      }
      return sb.toString();
    } finally {
      br.close();
    }
  }
  
  public static byte[] readSmallBinaryFile(final String aInputFileName) {
    final File file = new File(aInputFileName);
    final byte[] result = new byte[(int) file.length()];
    try {
      InputStream input = null;
      try {
        int totalBytesRead = 0;
        input = new BufferedInputStream(new FileInputStream(file));
        while (totalBytesRead < result.length) {
          final int bytesRemaining = result.length - totalBytesRead;
          // input.read() returns -1, 0, or more :
          final int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
          if (bytesRead > 0) {
            totalBytesRead = totalBytesRead + bytesRead;
          }
        }
        /* the above style is a bit tricky: it places bytes into the 'result'
         * array; 'result' is an output parameter; the while loop usually has a
         * single iteration only. */
      } finally {
        if (input != null) {
          input.close();
        }
      }
    } catch (final IOException ex) {
      ex.printStackTrace();
    }
    return result;
  }
  
  public static void writeSmallBinaryFile(final byte[] aBytes, final String aFileName) {
    try {
      OutputStream output = null;
      try {
        output = new BufferedOutputStream(new FileOutputStream(aFileName));
        output.write(aBytes);
      } finally {
        if (output != null) {
          output.close();
        }
      }
    } catch (final IOException ex) {
      ex.printStackTrace();
    }
  }
  
  public static boolean isTerminated(final Process p) {
    try {
      p.exitValue();
      return true;
    } catch (final IllegalThreadStateException e) {
      return false;
    }
  }
  
  public static long getRound() {
    return getMovieTime() > 0 ? (getMovieTime() / Common.currentConfiguration.cycleLength)
        : (getMovieTime() / Common.currentConfiguration.cycleLength) - 1;
  }
  
  public static <K extends Comparable<K>, V> void retainOnlyNewest(final int numToRetain, final Map<K, V> map) {
    while (map.size() > numToRetain) {
      map.remove(Collections.min(map.keySet()));
    }
  }
  
  public static int averageInt(final Collection<Integer> col) {
    int retVal = 0;
    for (final int a : col) {
      retVal += a;
    }
    return retVal / col.size();
  }
  
  public static long averageLong(final Collection<Long> col) {
    long retVal = 0;
    for (final long a : col) {
      retVal += a;
    }
    return retVal / col.size();
  }
  
  public static long getRandomSeed(final Random r) {
    try {
      final Field field = Random.class.getDeclaredField("seed");
      field.setAccessible(true);
      final AtomicLong scrambledSeed = (AtomicLong) field.get(r);
      return scrambledSeed.get() ^ 0x5DEECE66DL;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
