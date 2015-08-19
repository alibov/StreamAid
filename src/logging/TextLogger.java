package logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.Message;
import utils.Utils;
import experiment.frameworks.NodeAddress;

/**
 * A simple logger that logs stings and messages
 * 
 * @author Alexander Libov
 * 
 */
public class TextLogger {
  private static BufferedWriter out;
  private static Map<String, BufferedWriter> nodeLog = new HashMap<String, BufferedWriter>();
  public static boolean disabled;
  public static boolean instantFlush = true;
  public static Set<String> logNodes = new HashSet<String>();
  
  public enum State {
    Sent, Received, Send_failed
  }
  
  public static synchronized void logMessage(final Message m, final State state) {
    if (disabled && logNodes.isEmpty()) {
      return;
    }
    try {
      final String type = state.toString();
      final String logString = "time: " + Utils.getMovieTime() + " size: " + Utils.getSize(m) + " (" + type + ") " + m.toString()
          + "\n";
      if (!disabled) {
        out.write(logString);
        if (instantFlush) {
          out.flush();
        }
      }
      if (logNodes.contains(m.sourceId.toString())) {
        checkLogExistence(m.sourceId.toString());
        nodeLog.get(m.sourceId.toString()).write(logString);
        if (instantFlush) {
          nodeLog.get(m.sourceId.toString()).flush();
        }
      }
      if (logNodes.contains(m.destID.toString())) {
        checkLogExistence(m.destID.toString());
        nodeLog.get(m.destID.toString()).write(logString);
        if (instantFlush) {
          nodeLog.get(m.destID.toString()).flush();
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static void checkLogExistence(final String node) throws IOException {
    if (!nodeLog.containsKey(node)) {
      final BufferedWriter bw = new BufferedWriter(new FileWriter(ObjectLogger.dirName + File.separator + node + ".nodelog"));
      nodeLog.put(node, bw);
    }
  }
  
  public static void closeAll() throws IOException {
    try {
      if (!disabled && out != null) {
        out.close();
        out = null;
      }
    } finally {
      for (final BufferedWriter wr : nodeLog.values()) {
        try {
          wr.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
      nodeLog.clear();
    }
  }
  
  public static void init(final String logName) throws IOException {
    if (disabled) {
      return;
    }
    FileWriter fstream;
    fstream = new FileWriter(ObjectLogger.dirName + File.separator + logName + ".messagelog");
    out = new BufferedWriter(fstream);
  }
  
  public static synchronized void log(final NodeAddress node, final String string) {
    if (disabled && logNodes.isEmpty()) {
      return;
    }
    try {
      final String str = "time: " + Utils.getMovieTime() + " " + node.toString() + " " + string;
      if (!disabled) {
        out.write(str);
        if (instantFlush) {
          out.flush();
        }
      }
      if (logNodes.contains(node.toString())) {
        checkLogExistence(node.toString());
        nodeLog.get(node.toString()).write(str);
        if (instantFlush) {
          nodeLog.get(node.toString()).flush();
        }
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
