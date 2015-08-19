package experiment.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import utils.Common;
import experiment.ExperimentConfiguration;

public class ExperimentRunner {
  public static void main(final String[] args) throws Exception {
    System.err.println("Exp runner running ver 14/12/14 02:33");
    if (args.length != 3 && args.length != 4) {
      throw new IllegalStateException("usage: program <xml-list file name> <exp-jar file name> <num-processes> (<java flags>)");
    }
    String flags = "";
    if (args.length == 4) {
      flags = args[3];
    }
    final int numProcs = Integer.valueOf(args[2]);
    final BufferedReader br = new BufferedReader(new FileReader(args[0]));
    String line = null;
    final ArrayList<ProcessStreams> processList = new ArrayList<ProcessStreams>();
    final RunManager rm = new LocalRunManager();
    while ((line = br.readLine()) != null) {
      Thread.sleep(1000 * 1);
      final ExperimentConfiguration expc = new ExperimentConfiguration(new File(line));
      Common.currentConfiguration = expc;
      rm.initFiles();
      while (processList.size() == numProcs) {
        Thread.sleep(1000 * 1);
        for (int i = 0; i < processList.size(); i++) {
          final ProcessStreams ps = processList.get(i);
          try {
            final int exVal = ps.proc.exitValue();
            if (ps.err != null) {
              ps.err.close();
            }
            if (ps.out != null) {
              ps.out.close();
            }
            if (exVal != 0) {
              System.err.println("exit value: " + exVal);
            }
            processList.remove(i);
            i--;
          } catch (final IllegalThreadStateException itx) {
            // itx.printStackTrace();
          }
        }
      }
      final String commandLine = "java " + flags + " -jar " + args[1] + " " + line;
      System.out.println("running: " + commandLine);
      final ProcessStreams ps = new ProcessStreams();
      ps.proc = Runtime.getRuntime().exec(commandLine);
      ps.err = new FileOutputStream(line + ".errorLog");
      ps.out = new FileOutputStream(line + ".outputLog");
      final StreamGobbler errorGobbler = new StreamGobbler(ps.proc.getErrorStream(), "", ps.err);
      // any output?
      final StreamGobbler outputGobbler = new StreamGobbler(ps.proc.getInputStream(), "", ps.out);
      // kick them off
      errorGobbler.start();
      outputGobbler.start();
      processList.add(ps);
    }
    br.close();
  }
}

class ProcessStreams {
  Process proc;
  FileOutputStream out = null;
  FileOutputStream err = null;
}

class StreamGobbler extends Thread {
  InputStream is;
  String type;
  OutputStream os;

  StreamGobbler(final InputStream is, final String type) {
    this(is, type, null);
  }

  StreamGobbler(final InputStream is, final String type, final OutputStream redirect) {
    this.is = is;
    this.type = type;
    os = redirect;
  }

  @Override public void run() {
    try {
      PrintWriter pw = null;
      if (os != null) {
        pw = new PrintWriter(os);
      }
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null) {
        if (pw != null) {
          pw.println(line);
          pw.flush();
        }
        // System.out.println(type + ">" + line);
      }
      if (pw != null) {
        pw.flush();
      }
    } catch (final IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
