package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import entites.NodeAvailability;

public class SopCastTraceConverter {
  private static Map<String/* node name */, NodeAvailability> nodeAvail = new HashMap<String, NodeAvailability>();
  
  /**
   * @param args
   * @throws IOException
   */
  public static void main(final String[] args) throws IOException {
    if (args.length != 3) {
      throw new IllegalArgumentException("usage: program <trace files dir> <crawlers names file> <output file name>");
    }
    final File dir = new File(args[0]);
    final File crawlers = new File(args[1]);
    final File out = new File(args[2]);
    if (!dir.exists() || !crawlers.exists() || !dir.isDirectory() || !crawlers.isFile()) {
      throw new IllegalArgumentException("usage: program <trace files dir> <crawlers names file>");
    }
    final BufferedReader br = new BufferedReader(new FileReader(crawlers));
    String line;
    while ((line = br.readLine()) != null) {
      final String[] split = line.split("\t");
      final String ip = split[0];
      final String name = split[1];
      final File crawler = new File(args[0] + File.separator + name);
      if (!crawler.exists()) {
        System.err.println("cant find " + crawler.getAbsolutePath());
      }
      try {
        readCrawler(crawler, ip);
      } catch (final IOException e) {
        System.err.println("exception while reading from " + crawler.getAbsolutePath());
        e.printStackTrace();
      }
    }
    br.close();
    System.out.println("done reading");
    final TreeMap<String, NodeAvailability> treeMap = new TreeMap<String, NodeAvailability>(nodeAvail);
    final BufferedWriter bw = new BufferedWriter(new FileWriter(out));
    for (final Entry<String, NodeAvailability> entry : treeMap.entrySet()) {
      if (entry.getValue().joinTime == entry.getValue().leaveTime) {
        continue;// remove single message nodes
      }
      bw.append(entry.getKey() + " " + entry.getValue().joinTime + " " + entry.getValue().leaveTime + "\n");
    }
    bw.close();
  }
  
  private static void readCrawler(final File crawler, final String ip) throws IOException {
    final BufferedReader br = new BufferedReader(new FileReader(crawler));
    String line;
    System.out.println("reading " + crawler);
    while ((line = br.readLine()) != null) {
      final String[] split = line.split(" ");
      final long time = Long.parseLong(split[0]) * 1000; // in ms
      final String newIp = split[1].equals(ip) ? split[2] : split[1];
      Utils.checkExistence(nodeAvail, newIp, new NodeAvailability(time, time));
      nodeAvail.get(newIp).joinTime = Math.min(time, nodeAvail.get(newIp).joinTime);
      nodeAvail.get(newIp).leaveTime = Math.max(time, nodeAvail.get(newIp).leaveTime);
    }
    br.close();
    System.out.println("done reading " + crawler);
  }
}
