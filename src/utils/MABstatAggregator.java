package utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.TreeMap;

import com.google.common.io.Files;

public class MABstatAggregator {
  public static class MABstat {
    int numberSwitches = 0;
    TreeMap<Integer, Integer> optionChosen = new TreeMap<Integer, Integer>();
    
    public MABstat() {
      for (final int i : new int[] { -1, 0, 1, 2, 3 }) {
        optionChosen.put(i, 0);
      }
    }
  }
  
  static TreeMap<String, MABstat> res = new TreeMap<String, MABstatAggregator.MABstat>();
  static int lastOption = -1;
  static int rounds = 0;
  private static int optCindex;
  
  public static void main(final String[] args) throws IOException {
    System.out.println("algo, numberOfSwitches, opt0,opt1,opt2,opt3");
    // final String[] dirs = new String[] { "MABAlgoTest1res", "MABAlgoTest2res"
    // };
    final String[] dirs = new String[] { "." };
    // final String[] confs = new String[] { "ch0u0-MAB-res.csv",
    // "ch0u1-MAB-res.csv", "ch1u0-MAB-res.csv", "ch1u1-MAB-res.csv" };
    final String[] confs = new String[] { "ch1u0-MAB-res.csv", "ch1u1-MAB-res.csv" };
    for (final String conf : confs) {
      for (final String dir : dirs) {
        parseOnefile(dir + File.separator + conf);
      }
    }
  }
  
  private static void parseOnefile(final String file) throws IOException {
    String currentAlgo = null;
    for (final String line : Files.readLines(new File(file), Charset.defaultCharset())) {
      if (!line.contains(",")) {
        if (currentAlgo != null) {
          System.out.print(currentAlgo + ", " + (double) res.get(currentAlgo).numberSwitches / rounds);
          for (final int i : new int[] { 0, 1, 2, 3 }) {
            System.out.print(", " + ((double) res.get(currentAlgo).optionChosen.get(i)) / rounds);
          }
          System.out.println();
        }
        rounds = 0;
        currentAlgo = line;
        res.put(currentAlgo, new MABstat());
        lastOption = -1;
        continue;
      }
      if (line.startsWith("round")) {
        optCindex = line.substring(0, line.indexOf("optionchosen")).split(",").length;
        continue;
      }
      final int opt = Integer.parseInt(line.split(",")[optCindex]);
      res.get(currentAlgo).optionChosen.put(opt, res.get(currentAlgo).optionChosen.get(opt) + 1);
      if (opt != lastOption && opt != -1 && lastOption != -1) {
        res.get(currentAlgo).numberSwitches++;
      }
      lastOption = opt;
      rounds++;
    }
    System.out.print(currentAlgo + ", " + (double) res.get(currentAlgo).numberSwitches / rounds);
    for (final int i : new int[] { 0, 1, 2, 3 }) {
      System.out.print(", " + ((double) res.get(currentAlgo).optionChosen.get(i)) / rounds);
    }
    System.out.println();
  }
}
