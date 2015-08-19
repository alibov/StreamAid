package utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.google.common.io.Files;

public class RewardsFitting {
  public static class Instance {
    String file;
    ArrayList<Double> infos = new ArrayList<Double>();
    ArrayList<Integer> weights = new ArrayList<Integer>();
    Double result;
  }
  
  public static void main(final String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalStateException("usage: program <filelist>>");
    }
    final String[] algos = { "CR", "CRA", "CI" };
    // final String[] algos = { "CI" };
    createarff(initInstances(args, algos), "all100.arff", algos);
    createFullarff(initInstances(args, algos), "all100full.arff", algos);
  }
  
  private static void createarff(final ArrayList<Instance> initInstances, final String name, final String[] algos)
      throws IOException {
    final FileWriter fw = new FileWriter(name);
    fw.write("@RELATION " + name + "\n");
    for (int i = 0; i < initInstances.get(0).infos.size(); ++i) {
      fw.write("@ATTRIBUTE " + algos[i % algos.length] + i / algos.length + " NUMERIC\n");
    }
    fw.write("@ATTRIBUTE res NUMERIC\n");
    fw.write("@DATA\n");
    for (final Instance inst : initInstances) {
      for (final Double inf : inst.infos) {
        fw.write(inf + ",");
      }
      fw.write(inst.result + "\n");
    }
    fw.close();
  }
  
  private static void createFullarff(final ArrayList<Instance> initInstances, final String name, final String[] algos)
      throws IOException {
    final FileWriter fw = new FileWriter("Full" + name);
    fw.write("@RELATION " + name + "\n");
    fw.write("@ATTRIBUTE churn {ch0,ch1}\n");
    fw.write("@ATTRIBUTE upbw {u0,u1}\n");
    fw.write("@ATTRIBUTE player {wait1,wait2,skip1,skip2}\n");
    fw.write("@ATTRIBUTE stream {ChainSaw,Coolstreaming}\n");
    fw.write("@ATTRIBUTE overlay {Coolstreaming4,Coolstreaming7,RandomMinSize,Araneola4,Araneola7,BSCAMP}\n");
    fw.write("@ATTRIBUTE pp {pull,pushpull}\n");
    for (int i = 0; i < initInstances.get(0).infos.size(); ++i) {
      fw.write("@ATTRIBUTE " + algos[i % algos.length] + i / algos.length + " NUMERIC\n");
    }
    fw.write("@ATTRIBUTE res NUMERIC\n");
    fw.write("@DATA\n");
    for (final Instance inst : initInstances) {
      fw.write(inst.file.contains("ch0") ? "ch0," : "ch1,");
      fw.write(inst.file.contains("u0") ? "u0," : "u1,");
      for (final String pl : new String[] { "wait1", "wait2", "skip1", "skip2" }) {
        if (inst.file.contains(pl)) {
          fw.write(pl + ",");
          break;
        }
      }
      fw.write(inst.file.contains("ChainSaw-") ? "ChainSaw," : "Coolstreaming,");
      for (final String overlay : new String[] { "Coolstreaming4", "Coolstreaming7", "RandomMinSize", "Araneola4", "Araneola7",
      "BSCAMP" }) {
        if (inst.file.contains(overlay)) {
          fw.write(overlay + ",");
          break;
        }
      }
      fw.write(inst.file.contains("pushpull") ? "pushpull," : "pull,");
      for (final Double inf : inst.infos) {
        fw.write(inf + ",");
      }
      fw.write(inst.result + "\n");
    }
    fw.close();
  }
  
  private static ArrayList<Instance> initInstances(final String[] args, final String[] algos) throws IOException {
    final ArrayList<String> algoFiles = new ArrayList<String>();
    final HashSet<String> algosSet = new HashSet<String>(Arrays.asList(algos));
    for (final int churn : new int[] { 0, 1 }) {
      for (final int ubw : new int[] { 0, 1 }) {
        for (final String pl : new String[] { "wait1", "wait2", "skip1", "skip2" }) {
          for (final String stream : new String[] { "ChainSaw", "Coolstreaming" }) {
            for (final String overlay : new String[] { "Coolstreaming4", "Coolstreaming7", "RandomMinSize", "Araneola4",
                "Araneola7", "BSCAMP" }) {
              for (final String pp : new String[] { "pull", "pushpull" }) {
                algoFiles.add("ch" + churn + "u" + ubw + "-" + pl + "-" + stream + "-" + overlay + "-" + pp + "-EpsilonGreedy");
              }
            }
          }
        }
      }
    }
    final int stepLength = 100;
    final int resStepLength = 100;
    final ArrayList<Instance> instances = new ArrayList<RewardsFitting.Instance>();
    for (int i = 0; i < algoFiles.size(); ++i) {
      final List<String> rewardFile = Files.readLines(new File(new File(args[0]).getParent() + File.separator + algoFiles.get(i)
          + "_IG_CR_CRA_CI.xml.outputLog.-PRR.csv"), Charset.defaultCharset());
      final List<String> resultFile = Files.readLines(new File(new File(args[0]).getParent() + File.separator + algoFiles.get(i)
          + "-lagRes" + resStepLength + ".tsv"), Charset.defaultCharset());
      outer: for (int j = 1; j < resultFile.size(); ++j) {
        final Instance inst = new Instance();
        inst.result = Double.parseDouble(resultFile.get(j));
        for (int k = 0; k < stepLength / 10; ++k) {
          if ((j - 1) * (stepLength / 10) + k >= rewardFile.size()) {
            break outer;
          }
          final String rew = rewardFile.get((j - 1) * (stepLength / 10) + k);
          inst.file = algoFiles.get(i);
          if (algosSet.contains("CR")) {
            inst.infos.add(Double.parseDouble(rew.split(",")[2].trim())); // CR
          }
          if (algosSet.contains("CRA")) {
            inst.infos.add(Double.parseDouble(rew.split(",")[3].trim())); // CRA
          }
          if (algosSet.contains("CI")) {
            inst.infos.add(Double.parseDouble(rew.split(",")[4].trim())); // CI
          }
          // inst.weights.add(Integer.parseInt(rew.split(",")[3].trim()));
        }
        instances.add(inst);
      }
    }
    return instances;
  }
}
