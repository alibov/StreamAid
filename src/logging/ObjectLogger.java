package logging;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import logging.logObjects.DataLog;
import utils.Common;
import utils.Utils;
import experiment.logAnalyzer.LogAnalyzer;

/**
 * A logger that saves data objects
 * 
 * @author Alexander Libov
 * 
 * @param <T>
 */
public abstract class ObjectLogger<T extends DataLog> {
  // public static boolean useFiles = false;
  public static Map<String, List<?>> objLists = new HashMap<String, List<?>>();
  public static String dirName = null;
  public static LogAnalyzer LA = null;
  public static Map<Integer, LogAnalyzer> grouptoLA = new TreeMap<Integer, LogAnalyzer>();
  
  static public <M extends DataLog> ObjectLogger<M> getLogger(final String name) {
    if (Common.currentConfiguration == null || Common.currentConfiguration.framework.isSimulator()) {
      return new InMemoryObjectLogger<M>(name);
    }
    return new FileObjectLogger<M>(name);
  }
  
  public static void initAll() {
    if (Common.currentConfiguration.framework.isSimulator()) {
      dirName = utils.Common.currentConfiguration.name + File.separator + Thread.currentThread().getId() + "t"
          + String.valueOf(new Date().getTime());
    } else {
      dirName = utils.Common.currentConfiguration.name + File.separator + Utils.getLocalHostName();
    }
    final File dir = new File(dirName);
    if (!dir.exists()) {
      dir.mkdir();
    }
    for (final ObjectLogger<?> log : loggers) {
      log.init();
    }
    LA = new LogAnalyzer(dirName);
    for (int i = 0; i < Common.currentConfiguration.getGroupNumber() && Common.currentConfiguration.getGroupNumber() > 1; i++) {
      grouptoLA.put(i, new LogAnalyzer(dirName, i));
    }
  }
  
  abstract protected void init();
  
  protected final String logName;
  protected static HashSet<ObjectLogger<?>> loggers = new HashSet<ObjectLogger<?>>();
  private static List<File> gatheredDirs = new LinkedList<File>();
  
  abstract public void logObject(final T m);
  
  public ObjectLogger(final String logName) {
    this.logName = logName;
    loggers.add(this);
  }
  
  public static void closeAll() {
    for (final ObjectLogger<?> ml : loggers) {
      ml.close();
    }
  }
  
  abstract protected void close();
  
  public static void addGatheredDir(final File file) {
    gatheredDirs.add(file);
  }
  
  public static void readFromGatheredDirs() {
    final Map<String, List<Object>> objectMap = new HashMap<String, List<Object>>();
    for (final File dir : gatheredDirs) {
      if (!dir.isDirectory()) {
        continue;
      }
      for (final File log : dir.listFiles()) {
        if (log.getName().contains("nodelog") || log.getName().contains("messagelog")) {
          continue;
        }
        final String name = log.getName();
        Utils.checkExistence(objectMap, name, new LinkedList<Object>());
        FileInputStream fstream;
        try {
          fstream = new FileInputStream(log);
          final ObjectInputStream in = new ObjectInputStream(fstream);
          Object obj = null;
          while ((obj = in.readObject()) != null) {
            objectMap.get(name).add(obj);
          }
          in.close();
        } catch (final EOFException e) {
          // do nothing! Thank you for an interface that requires you to catch
          // exceptions during normal operation!
          // and no, ObjectInputStream.available does not help.
        } catch (final IOException e) {
          System.err.println("dir " + dir + " caught IOException processing " + log);
          e.printStackTrace();
        } catch (final Exception e) {
          System.err.println("dir " + dir + " caught Exception processing " + log);
          e.printStackTrace();
        }
      }
    }
    for (final String str : objectMap.keySet()) {
      objLists.put(str, objectMap.get(str));
    }
  }
}
