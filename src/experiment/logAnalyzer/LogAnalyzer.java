package experiment.logAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.ObjectLogger;
import logging.logObjects.AvailabilityLog;
import logging.logObjects.AvailabilityLog.State;
import logging.logObjects.BandwidthLog;
import logging.logObjects.ChunkGenerationLog;
import logging.logObjects.ChunkPlayLog;
import logging.logObjects.ChunkReceiveLog;
import logging.logObjects.ChunkSendLog;
import logging.logObjects.ChurnLog;
import logging.logObjects.DegreeLog;
import logging.logObjects.OptionLog;
import logging.logObjects.SendLog;
import modules.player.VideoStream;
import utils.Common;
import utils.Statistics;
import utils.Utils;

/**
 * stores logs of experiment runs and analyzes them
 *
 * @author Alexander Libov
 *
 */
public class LogAnalyzer implements Serializable {
  private static final long serialVersionUID = -3227406414017675340L;
  public final String dirName;
  public int descriptors = 1;
  private final Map<String /* node */, NodeInfo> nodeInfo = new TreeMap<String, NodeInfo>();
  private final Map<String /* protocol */, ProtocolInfo> protocolInfo = new TreeMap<String, ProtocolInfo>();
  private transient final Map<Long/* chunk */, Long/* time */> chunkGenerationTime = new TreeMap<Long, Long>();
  private transient final Map<Long/* chunk */, Map<Integer/* descriptor */, Map<String/* source
   * node */, Set<String>/* dest
   * nodes */>>> chunkReceiveMap = new TreeMap<Long, Map<Integer, Map<String, Set<String>>>>();
  private final List<Long> totalUploadBandwidth = new LinkedList<Long>();
  private final ArrayList<Long> avgUploadBandwidth = new ArrayList<Long>();
  private final ArrayList<Integer> avgUploadBandwidthCount = new ArrayList<Integer>();
  private final List<Long> totalServerUploadBandwidth = new LinkedList<Long>();
  private final Map<Integer/* order */, OrderInfo> orderInfo = new TreeMap<Integer, OrderInfo>();
  private String serverId = null;
  public int maxOrder = 0;
  private transient String separator = "\t";
  private transient String fileExtension;
  private long averageLag;
  private long averageStartupDelay;
  private double averageHopcount;
  private final Statistics averageNodeLatency = new Statistics();
  private final Statistics averageNodeLatencySD = new Statistics();
  private long averageLastDelay;
  private double averageQuality;
  private final Statistics continuityIndex = new Statistics();
  private final Statistics simpleContinuityIndex = new Statistics();
  private int maxGroup = 0;
  private double perfectCIpercentage;
  private final Statistics ZeroCI = new Statistics();
  private final Statistics ZeroCITime = new Statistics();
  private final Statistics continuityIndexSD = new Statistics();
  private final Map<Long, SecondInfo> secondInfo = new TreeMap<Long, SecondInfo>();
  private final Map<Integer, PeriodInfo> periodInfo = new TreeMap<Integer, PeriodInfo>();
  private int maxOption = 0;
  private final Map<Integer/* alg */, Map<Long, UptimeInfo>> uptimeInfo = new TreeMap<Integer, Map<Long, UptimeInfo>>();
  private final Map<Long, Long> chunkIdPlayed = new TreeMap<Long, Long>();
  private int analyzerCount = 1;
  private transient Integer algFilter = null;
  
  // TODO each chunk creates a tree. create a visualization of that tree.
  public LogAnalyzer(final String dirName) {
    this.dirName = dirName;
  }
  
  public LogAnalyzer(final String dirName, final int algFilter) {
    this(dirName);
    this.algFilter = algFilter;
  }
  
  public LogAnalyzer(final Collection<String> analyzers, final Integer group) {
    String addition = "";
    if (group != null) {
      addition += group;
    }
    dirName = Common.currentConfiguration.name + File.separator + Common.currentConfiguration.toString() + "averageOf"
        + analyzers.size();
    final File dir = new File(dirName);
    dir.mkdir();
    final Map<Integer/* order */, Integer/* count */> orderCount = new HashMap<Integer, Integer>();
    final Map<String/* protocol */, Integer/* count */> protocolCount = new HashMap<String, Integer>();
    long averageLagSum = 0;
    long averageStartupDelaySum = 0;
    double averageHopcountSum = 0;
    long averageLastDelaySum = 0;
    double averageQualitySum = 0;
    for (final String analyzerPath : analyzers) {
      LogAnalyzer analyzer = null;
      analyzer = LogAnalyzer.retrieve(analyzerPath + addition);
      maxOrder = Math.max(maxOrder, analyzer.maxOrder);
      maxOption = Math.max(maxOption, analyzer.maxOption);
      for (final Integer i : analyzer.orderInfo.keySet()) {
        Utils.checkExistence(orderInfo, i, new OrderInfo());
        Utils.checkExistence(orderCount, i, 0);
        orderInfo.get(i).averageLatency += analyzer.orderInfo.get(i).averageLatency;
        orderInfo.get(i).count += analyzer.orderInfo.get(i).count;
        orderCount.put(i, orderCount.get(i) + 1);
      }
      for (final String i : analyzer.protocolInfo.keySet()) {
        Utils.checkExistence(protocolInfo, i, new ProtocolInfo());
        Utils.checkExistence(protocolCount, i, 0);
        final ProtocolInfo p = protocolInfo.get(i);
        p.bytesSent += analyzer.protocolInfo.get(i).bytesSent;
        p.messagesSent += analyzer.protocolInfo.get(i).messagesSent;
        p.overheadBytesSent += analyzer.protocolInfo.get(i).overheadBytesSent;
        p.overheadMessagesSent += analyzer.protocolInfo.get(i).overheadMessagesSent;
        p.duplicateChunkBytesSent += analyzer.protocolInfo.get(i).duplicateChunkBytesSent;
        p.duplicateChunkMessagesSent += analyzer.protocolInfo.get(i).duplicateChunkMessagesSent;
        p.averageAverageDegree += analyzer.protocolInfo.get(i).averageAverageDegree;
        p.averageNodeDegreeVariance += analyzer.protocolInfo.get(i).averageNodeDegreeVariance;
        p.degreeVariance += analyzer.protocolInfo.get(i).degreeVariance;
        p.serverAverageDegree += analyzer.protocolInfo.get(i).serverAverageDegree;
        p.serverDegreeVariance += analyzer.protocolInfo.get(i).serverDegreeVariance;
        protocolCount.put(i, protocolCount.get(i) + 1);
      }
      for (final Long i : analyzer.secondInfo.keySet()) {
        Utils.checkExistence(secondInfo, i, new SecondInfo());
        final SecondInfo si = secondInfo.get(i);
        si.allBitsInQueue += analyzer.secondInfo.get(i).allBitsInQueue;
        si.allUploadUtilization += analyzer.secondInfo.get(i).allUploadUtilization;
        si.serverBitsInQueue += analyzer.secondInfo.get(i).serverBitsInQueue;
        si.serverUploadUtilization += analyzer.secondInfo.get(i).serverUploadUtilization;
        si.availablenodes += analyzer.secondInfo.get(i).availablenodes;
        for (final State s : AvailabilityLog.State.values()) {
          Utils.checkExistence(si.nodesInState, s, 0);
          if (analyzer.secondInfo.get(i).nodesInState.containsKey(s)) {
            si.nodesInState.put(s, si.nodesInState.get(s) + analyzer.secondInfo.get(i).nodesInState.get(s));
          }
        }
        si.latencyAvg += analyzer.secondInfo.get(i).latencyAvg;
        // si.chunksPlayed += analyzer.secondInfo.get(i).chunksPlayed;
        for (final int opt : analyzer.secondInfo.get(i).optionChosen.keySet()) {
          Utils.checkExistence(si.optionChosen, opt, 0);
          si.optionChosen.put(opt, si.optionChosen.get(opt) + analyzer.secondInfo.get(i).optionChosen.get(opt));
        }
        si.optionChange += analyzer.secondInfo.get(i).optionChange;
      }
      for (final Integer i : analyzer.periodInfo.keySet()) {
        Utils.checkExistence(periodInfo, i, new PeriodInfo());
        final PeriodInfo pi = periodInfo.get(i);
        for (final int opt : analyzer.periodInfo.get(i).optionChosen.keySet()) {
          Utils.checkExistence(pi.optionChosen, opt, 0);
          pi.optionChosen.put(opt, pi.optionChosen.get(opt) + analyzer.periodInfo.get(i).optionChosen.get(opt));
        }
        pi.optionChange += analyzer.periodInfo.get(i).optionChange;
        pi.nodesStartingPeriod += analyzer.periodInfo.get(i).nodesStartingPeriod;
      }
      for (final Integer alg : analyzer.uptimeInfo.keySet()) {
        Utils.checkExistence(uptimeInfo, alg, new TreeMap<Long, UptimeInfo>());
        for (final Long i : analyzer.uptimeInfo.get(alg).keySet()) {
          Utils.checkExistence(uptimeInfo.get(alg), i, new UptimeInfo());
          final UptimeInfo ui = uptimeInfo.get(alg).get(i);
          final UptimeInfo otherUI = analyzer.uptimeInfo.get(alg).get(i);
          for (final String protocol : otherUI.protocolDegree.keySet()) {
            Utils.checkExistence(ui.protocolDegree, protocol, new Statistics());
            ui.protocolDegree.get(protocol).addWeightedDatum(otherUI.protocolDegree.get(protocol).dataSize(),
                otherUI.protocolDegree.get(protocol).getMean());
          }
        }
      }
      averageLagSum += analyzer.averageLag;
      averageStartupDelaySum += analyzer.averageStartupDelay;
      averageHopcountSum += analyzer.averageHopcount;
      averageNodeLatency.addDatum(analyzer.averageNodeLatency.getMean());
      averageNodeLatencySD.addDatum(analyzer.averageNodeLatency.getStdDev());
      averageQualitySum += analyzer.averageQuality;
      perfectCIpercentage += analyzer.perfectCIpercentage;
      ZeroCI.addWeightedDatum(analyzer.ZeroCI.dataSize(), analyzer.ZeroCI.getMean());
      ZeroCITime.addDatum(analyzer.ZeroCITime.getMean());
      continuityIndex.addDatum(analyzer.continuityIndex.getMean());
      simpleContinuityIndex.addDatum(analyzer.simpleContinuityIndex.getMean());
      continuityIndexSD.addDatum(analyzer.continuityIndex.getStdDev());
      averageLastDelaySum += analyzer.averageLastDelay;
      for (final Entry<Long, Long> entry : analyzer.chunkIdPlayed.entrySet()) {
        Utils.checkExistence(chunkIdPlayed, entry.getKey(), 0L);
        chunkIdPlayed.put(entry.getKey(), chunkIdPlayed.get(entry.getKey()) + entry.getValue());
      }
      for (final Entry<String, NodeInfo> ni : analyzer.nodeInfo.entrySet()) {
        if (analyzer.serverId.equals(ni.getKey())) {
          totalServerUploadBandwidth.add(ni.getValue().totalUsedUploadBandwidth);
        } else {
          totalUploadBandwidth.add(ni.getValue().totalUsedUploadBandwidth);
          analyzer.avgUploadBandwidth.add(ni.getValue().totalUsedUploadBandwidth);
        }
      }
      Collections.sort(analyzer.avgUploadBandwidth, Collections.reverseOrder());
      if (avgUploadBandwidth.isEmpty()) {
        avgUploadBandwidth.addAll(analyzer.avgUploadBandwidth);
        for (int i = 0; i < analyzer.avgUploadBandwidth.size(); ++i) {
          avgUploadBandwidthCount.add(1);
        }
      } else {
        int i = 0;
        for (; i < avgUploadBandwidth.size(); i++) {
          if (i >= analyzer.avgUploadBandwidth.size()) {
            break;
          }
          avgUploadBandwidth.set(i, avgUploadBandwidth.get(i) + analyzer.avgUploadBandwidth.get(i));
          avgUploadBandwidthCount.set(i, avgUploadBandwidthCount.get(i) + 1);
        }
        for (; i < analyzer.avgUploadBandwidth.size(); ++i) {
          avgUploadBandwidth.add(analyzer.avgUploadBandwidth.get(i));
          avgUploadBandwidthCount.add(1);
        }
      }
    }
    Collections.sort(totalUploadBandwidth, Collections.reverseOrder());
    Collections.sort(totalServerUploadBandwidth, Collections.reverseOrder());
    for (int i = 0; i < avgUploadBandwidth.size(); i++) {
      avgUploadBandwidth.set(i, avgUploadBandwidth.get(i) / avgUploadBandwidthCount.get(i));
    }
    perfectCIpercentage /= analyzers.size();
    averageLag = averageLagSum / analyzers.size();
    averageStartupDelay = averageStartupDelaySum / analyzers.size();
    averageHopcount = averageHopcountSum / analyzers.size();
    averageLastDelay = averageLastDelaySum / analyzers.size();
    averageQuality = averageQualitySum / analyzers.size();
    for (final Integer i : orderInfo.keySet()) {
      orderInfo.get(i).averageLatency /= orderCount.get(i);
      orderInfo.get(i).count /= orderCount.get(i);
    }
    for (final String i : protocolInfo.keySet()) {
      final int count = protocolCount.get(i);
      final ProtocolInfo pc = protocolInfo.get(i);
      pc.bytesSent /= count;
      pc.messagesSent /= count;
      pc.overheadBytesSent /= count;
      pc.overheadMessagesSent /= count;
      pc.duplicateChunkBytesSent /= count;
      pc.duplicateChunkMessagesSent /= count;
      pc.averageAverageDegree /= count;
      pc.averageNodeDegreeVariance /= count;
      pc.degreeVariance /= count;
      pc.serverAverageDegree /= count;
      pc.serverDegreeVariance /= count;
    }
    for (final Long i : secondInfo.keySet()) {
      final SecondInfo si = secondInfo.get(i);
      si.serverBitsInQueue /= analyzers.size();
      si.serverUploadUtilization /= analyzers.size();
      // si.chunksPlayed /= analyzers.size();
      si.availablenodes /= analyzers.size();
      si.latencyAvg /= analyzers.size();
      si.allBitsInQueue /= analyzers.size();
      si.allUploadUtilization /= analyzers.size();
      for (final int opt : si.optionChosen.keySet()) {
        si.optionChosen.put(opt, si.optionChosen.get(opt) / analyzers.size());
      }
      si.optionChange /= analyzers.size();
    }
    for (final Integer i : periodInfo.keySet()) {
      final PeriodInfo pi = periodInfo.get(i);
      for (final int opt : pi.optionChosen.keySet()) {
        pi.optionChosen.put(opt, pi.optionChosen.get(opt) / analyzers.size());
      }
      pi.optionChange /= analyzers.size();
      pi.nodesStartingPeriod /= analyzers.size();
    }
    for (final Entry<Long, Long> entry : chunkIdPlayed.entrySet()) {
      chunkIdPlayed.put(entry.getKey(), entry.getValue() / analyzers.size());
    }
    analyzerCount = analyzers.size();
  }
  
  public void handleLogObject(final Object m, final String logName) {
    if (m instanceof AvailabilityLog) {
      handleAvailabilityLog((AvailabilityLog) m);
    } else if (m instanceof ChurnLog) {
      handleChurnLog((ChurnLog) m);
    } else if (m instanceof ChunkGenerationLog) {
      handleChunkGenerationLog((ChunkGenerationLog) m);
    } else if (m instanceof ChunkPlayLog) {
      handleChunkPlayLog((ChunkPlayLog) m);
    } else if (m instanceof ChunkReceiveLog) {
      handleChunkReceiveLog((ChunkReceiveLog) m);
    } else if (m instanceof BandwidthLog) {
      handleBandwidthLog((BandwidthLog) m);
    } else if (m instanceof SendLog) {
      handleSendLog((SendLog) m);
    } else if (logName.equals("degreeLog")) {
      handleDegreeLog((DegreeLog) m);
    } else if (logName.equals("secDegreeLog")) {
      handleSecDegreeLog((DegreeLog) m);
    } else if (logName.equals("puloptlog")) {
      handlePullOptionLog((OptionLog) m);
    } else {
      throw new IllegalStateException("unrecognised object " + m + "in log: " + logName);
    }
  }
  
  public void analyze() {
    if (ObjectLogger.objLists.containsKey("availLog")) {
      for (final Object sid : ObjectLogger.objLists.get("availLog")) {
        handleAvailabilityLog((AvailabilityLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("churnLog")) {
      for (final Object sid : ObjectLogger.objLists.get("churnLog")) {
        handleChurnLog((ChurnLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("chunkGen")) {
      for (final Object sid : ObjectLogger.objLists.get("chunkGen")) {
        handleChunkGenerationLog((ChunkGenerationLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("chunkPlay")) {
      for (final Object sid : ObjectLogger.objLists.get("chunkPlay")) {
        handleChunkPlayLog((ChunkPlayLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("chunkRec")) {
      for (final Object sid : ObjectLogger.objLists.get("chunkRec")) {
        handleChunkReceiveLog((ChunkReceiveLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("bandLog")) {
      for (final Object sid : ObjectLogger.objLists.get("bandLog")) {
        handleBandwidthLog((BandwidthLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("sendlog")) {
      for (final Object sid : ObjectLogger.objLists.get("sendlog")) {
        handleSendLog((SendLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("degreeLog")) {
      for (final Object sid : ObjectLogger.objLists.get("degreeLog")) {
        handleDegreeLog((DegreeLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("secDegreeLog")) {
      for (final Object sid : ObjectLogger.objLists.get("secDegreeLog")) {
        handleSecDegreeLog((DegreeLog) sid);
      }
    }
    if (ObjectLogger.objLists.containsKey("puloptlog")) {
      for (final Object sid : ObjectLogger.objLists.get("puloptlog")) {
        handlePullOptionLog((OptionLog) sid);
      }
    }
    calculateAverageOrder();
    calculateAverageLatency();
    calculateAverageLatencyPerOrder();
    calculateStartupDelay();
    calculateDegreeVariance();
  }
  
  private transient Map<String, Integer> lastOption = new HashMap<String, Integer>();
  private transient Map<Integer, Set<String>> nodesInPeriod = new HashMap<Integer, Set<String>>();
  
  private void handlePullOptionLog(final OptionLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    final long second = sid.time / 1000;
    Utils.checkExistence(secondInfo.get(second).optionChosen, sid.chosenOption, 0);
    secondInfo.get(second).optionChosen.put(sid.chosenOption, secondInfo.get(second).optionChosen.get(sid.chosenOption) + 1);
    Utils.checkExistence(periodInfo, sid.currentPeriod, new PeriodInfo());
    Utils.checkExistence(nodesInPeriod, sid.currentPeriod, new HashSet<String>());
    Utils.checkExistence(periodInfo.get(sid.currentPeriod).optionChosen, sid.chosenOption, 0);
    if (!nodesInPeriod.get(sid.currentPeriod).contains(sid.node)) {
      periodInfo.get(sid.currentPeriod).optionChosen.put(sid.chosenOption,
          periodInfo.get(sid.currentPeriod).optionChosen.get(sid.chosenOption) + 1);
      periodInfo.get(sid.currentPeriod).nodesStartingPeriod++;
      nodesInPeriod.get(sid.currentPeriod).add(sid.node);
    }
    if (sid.chosenOption > maxOption) {
      maxOption = sid.chosenOption;
    }
    Utils.checkExistence(lastOption, sid.node, sid.chosenOption);
    if (!lastOption.get(sid.node).equals(sid.chosenOption)) {
      periodInfo.get(sid.currentPeriod).optionChange++;
      secondInfo.get(second).optionChange++;
      lastOption.put(sid.node, sid.chosenOption);
    }
  }
  
  private void handleSecDegreeLog(final DegreeLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    final int alg = nodeInfo.get(sid.node).alg;
    Utils.checkExistence(uptimeInfo, alg, new TreeMap<Long, UptimeInfo>());
    Utils.checkExistence(uptimeInfo.get(alg), sid.duration, new UptimeInfo());
    final UptimeInfo ui = uptimeInfo.get(alg).get(sid.duration);
    Utils.checkExistence(ui.protocolDegree, sid.protocol, new Statistics());
    ui.protocolDegree.get(sid.protocol).addDatum(sid.degree);
  }
  
  private void calculateDegreeVariance() {
    for (final ProtocolInfo protocol : protocolInfo.values()) {
      if (protocol.nodeDegree.isEmpty()) {
        continue;
      }
      final Statistics averageDegree = new Statistics();
      final Statistics averageVariance = new Statistics();
      for (final Statistics stat : protocol.nodeDegree.values()) {
        averageDegree.addDatum(stat.getMean());
        averageVariance.addDatum(stat.getVariance());
      }
      protocol.averageAverageDegree = averageDegree.getMean();
      protocol.averageNodeDegreeVariance = averageVariance.getMean();
      protocol.degreeVariance = averageDegree.getVariance();
      protocol.serverAverageDegree = protocol.serverStats.getMean();
      protocol.serverDegreeVariance = protocol.serverStats.getVariance();
    }
  }
  
  private void handleDegreeLog(final DegreeLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    Utils.checkExistence(protocolInfo, sid.protocol, new ProtocolInfo());
    final ProtocolInfo info = protocolInfo.get(sid.protocol);
    final long lifeTime = nodeInfo.get(sid.node.toString()).leaveTime - nodeInfo.get(sid.node.toString()).joinTime;
    if (sid.node.toString().equals(serverId)) {
      info.serverStats.addWeightedDatum(((double) sid.duration / lifeTime), sid.degree);
      return;
    }
    try {
      Utils.checkExistence(info.nodeDegree, sid.node.toString(), new Statistics());
    } catch (final Throwable t) {
      t.printStackTrace();
    }
    info.nodeDegree.get(sid.node.toString()).addWeightedDatum(((double) sid.duration / lifeTime), sid.degree);
  }
  
  private void handleBandwidthLog(final BandwidthLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    nodeInfo.get(sid.node).totalUsedUploadBandwidth += sid.usedBandwidth;
    final long second = sid.time / 1000;
    Utils.checkExistence(secondInfo, second, new SecondInfo());
    if (sid.node.toString().equals(serverId)) {
      secondInfo.get(second).serverBitsInQueue = sid.bitsInWaitQueue;
      secondInfo.get(second).serverUploadUtilization = sid.usedBandwidth;
    } else {
      secondInfo.get(second).allBitsInQueue += sid.bitsInWaitQueue;
      secondInfo.get(second).allUploadUtilization += sid.usedBandwidth;
      secondInfo.get(second).availablenodes++;
    }
  }
  
  private void calculateStartupDelay() {
    final Statistics lagStats = new Statistics();
    final Statistics delayStats = new Statistics();
    final Statistics lastDelayStats = new Statistics();
    final Statistics avgHopcountStats = new Statistics();
    for (final String node : nodeInfo.keySet()) {
      final NodeInfo info = nodeInfo.get(node);
      if (info.chunkInfo.isEmpty()) {
        continue;
      }
      final long firstChunkPlayed = info.chunkInfo.keySet().iterator().next();
      info.startupDelay = info.chunkInfo.get(firstChunkPlayed).playTime
          - Math.max(info.joinTime - Utils.movieStartTime, chunkGenerationTime.get(VideoStream.startingFrame));
      info.lag = info.chunkInfo.get(info.lastChunkPlayed).playLatency - info.chunkInfo.get(firstChunkPlayed).playLatency;
      info.lastDelay = info.leaveTime - Utils.movieStartTime - info.chunkInfo.get(info.lastChunkPlayed).playTime;
      if (info.startupDelay < 0) {
        // throw new RuntimeException("illegal results! for node " + node +
        // ": startupDelay = " + info.startupDelay);
        System.out.println("illegal results! for node " + node + ": startupDelay = " + info.startupDelay);
      }
      lagStats.addDatum(info.lag, node);
      delayStats.addDatum(info.startupDelay, node);
      lastDelayStats.addDatum(info.lastDelay, node);
      if (info.chunkOrderAvg != -1) {
        avgHopcountStats.addDatum(info.chunkOrderAvg, node);
      }
      if (info.lastDelay > 10000) {
        printNodeInfo(node);
      }
    }
    averageHopcount = avgHopcountStats.getMean();
    averageLag = (long) lagStats.getMean();
    averageStartupDelay = (long) delayStats.getMean();
    averageLastDelay = (long) lastDelayStats.getMean();
    if (avgHopcountStats.dataSize() == 1.0 || avgHopcountStats.getMaxItem() == null) {
      throw new RuntimeException("no chunks were delivered!!");
    }
    System.err.print("max hopcount: ");
    printNodeInfo(avgHopcountStats.getMaxItem());
    System.err.print("max lag: ");
    printNodeInfo(lagStats.getMaxItem());
    System.err.print("max delay: ");
    printNodeInfo(delayStats.getMaxItem());
    System.err.print("max lastDelay: ");
    printNodeInfo(lastDelayStats.getMaxItem());
  }
  
  public void printFullnodeInfo() {
    for (final String node : nodeInfo.keySet()) {
      printNodeInfo(node);
    }
  }
  
  private void printNodeInfo(final String node) {
    final NodeInfo info = nodeInfo.get(node);
    if (!info.chunkInfo.isEmpty()) {
      final long firstChunkPlayed = info.chunkInfo.keySet().iterator().next();
      System.err.print("node " + node + " startup delay: " + info.startupDelay + ", startup latency: "
          + info.chunkInfo.get(firstChunkPlayed).playLatency + " , lag: " + info.lag);
      System.err.println(" last delay: " + info.lastDelay + " avg hopcount: " + info.chunkOrderAvg + " up from " + info.joinTime
          + " to " + info.leaveTime + " (" + (info.leaveTime - info.joinTime) + " ms)");
    } else {
      System.err.println("node " + node + " didn't play any chunks! and was up from " + info.joinTime + " to " + info.leaveTime
          + " (" + (info.leaveTime - info.joinTime) + " ms)");
    }
  }
  
  private void handleChurnLog(final ChurnLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    if (sid.joined) {
      Utils.checkExistence(nodeInfo, sid.node, new NodeInfo());
      nodeInfo.get(sid.node).joinTime = sid.time;
      nodeInfo.get(sid.node).leaveTime = Long.MAX_VALUE;
      nodeInfo.get(sid.node).alg = sid.alg;
      if (sid.alg > maxGroup) {
        maxGroup = sid.alg;
      }
      nodeInfo.get(sid.node).startupBuffer = sid.startupBuffer;
      nodeInfo.get(sid.node).cycle = sid.cycle;
      nodeInfo.get(sid.node).bufferFromFirstChunk = sid.bufferFromFirstChunk;
    } else {
      nodeInfo.get(sid.node.toString()).leaveTime = sid.time;
    }
  }
  
  public void writeLatencyPerOrder(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#per order\n");
      fw.write("#hopCount" + separator + "average latency" + separator + "number of chunks\n");
      for (int i = 0; i < maxOrder + 1; i++) {
        final OrderInfo info = orderInfo.get(i);
        fw.write(i + separator + info.averageLatency + separator + info.count + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeDataUsage(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#data usage\n");
      fw.write("#protocol" + separator + "overhead messages" + separator + "overall messages" + separator + "overhead bytes"
          + separator + "overall bytes" + separator);
      fw.write("averageAverageDegree" + separator + "averageNodeDegreeVariance" + separator + "degreeVariance" + separator
          + "serverAverageDegree" + separator + "serverDegreeVariance\n");
      for (final String prot : protocolInfo.keySet()) {
        final ProtocolInfo info = protocolInfo.get(prot);
        fw.write(prot + separator + info.overheadMessagesSent + separator + info.duplicateChunkMessagesSent + separator
            + info.messagesSent);
        fw.write(separator + info.overheadBytesSent + separator + info.duplicateChunkBytesSent + separator + info.bytesSent);
        fw.write(separator + info.averageAverageDegree + separator + info.averageNodeDegreeVariance);
        fw.write(separator + info.degreeVariance + separator + info.serverAverageDegree + separator + info.serverDegreeVariance
            + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeSecondInfo(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#second info\n");
      fw.write("#movie second" + separator + "server Upload Utilization" + separator + "server Bits In Queue" + separator
          + "all Upload Utilization" + separator + "all Bits In Queue" + separator + "available nodes" + separator + "latency");
      for (final State s : State.values()) {
        fw.write(separator + s.name());
      }
      for (int i = 0; i <= maxOption; ++i) {
        fw.write(separator + "option" + i);
      }
      fw.write(separator + "option change");
      fw.write("\n");
      for (final Long i : secondInfo.keySet()) {
        final SecondInfo info = secondInfo.get(i);
        if (info.availablenodes == 0) {
          continue;
        }
        double latency = -1;
        if (info.nodesInState.get(State.PLAYING) > 0) {
          latency = info.latencyAvg / info.nodesInState.get(State.PLAYING);
        }
        fw.write(i + separator + info.serverUploadUtilization + separator + info.serverBitsInQueue);
        fw.write(separator + info.allUploadUtilization / info.availablenodes + separator + info.allBitsInQueue
            / info.availablenodes);
        fw.write(separator + info.availablenodes);
        fw.write(separator + latency);
        for (final State s : State.values()) {
          fw.write(separator + info.nodesInState.get(s));
        }
        for (int j = 0; j <= maxOption; ++j) {
          Integer num = info.optionChosen.get(j);
          if (num == null) {
            num = 0;
          }
          fw.write(separator + ((double) num) / info.availablenodes);
        }
        fw.write(separator + info.optionChange);
        fw.write("\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writePeriodInfo(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#period info\n");
      fw.write("#period" + separator + "available nodes");
      for (int i = 0; i <= maxOption; ++i) {
        fw.write(separator + "option" + i);
      }
      fw.write(separator + "option change");
      fw.write("\n");
      for (final Integer i : periodInfo.keySet()) {
        final PeriodInfo info = periodInfo.get(i);
        if (info.nodesStartingPeriod == 0) {
          continue;
        }
        fw.write(i + separator + info.nodesStartingPeriod);
        for (int j = 0; j <= maxOption; ++j) {
          Integer num = info.optionChosen.get(j);
          if (num == null) {
            num = 0;
          }
          fw.write(separator + ((double) num) / info.nodesStartingPeriod);
        }
        fw.write(separator + info.optionChange);
        fw.write("\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeUploadBandwidth(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + ".dat"));
      fw.write("#UploadBandwidth\n");
      for (final Long i : totalUploadBandwidth) {
        fw.write(i + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeAvgUploadBandwidth(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + ".dat"));
      fw.write("#AvgUploadBandwidth\n");
      for (final Long i : avgUploadBandwidth) {
        fw.write(i + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeServerUploadBandwidth(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + ".dat"));
      fw.write("#ServerUploadBandwidth\n");
      for (final Long i : totalServerUploadBandwidth) {
        fw.write(i + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public void writeUptimeInfo(final String filename) {
    for (final Integer alg : uptimeInfo.keySet()) {
      try {
        final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + alg + "." + fileExtension));
        fw.write("#uptime info\n");
        fw.write("#uptime second");
        for (final String prot : uptimeInfo.get(alg).get(1L).protocolDegree.keySet()) {
          fw.write(separator + prot + "degree");
        }
        fw.write(separator + "count\n");
        for (final Long i : uptimeInfo.get(alg).keySet()) {
          final UptimeInfo info = uptimeInfo.get(alg).get(i);
          fw.write(i + separator);
          double count = 0;
          for (final Statistics degree : info.protocolDegree.values()) {
            fw.write(degree.getMean() + separator);
            count = degree.dataSize();
          }
          fw.write(count / analyzerCount + "\n");
        }
        fw.close();
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void writechunkIdInfo(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#chunkID info\n");
      fw.write("#chunk ID" + separator + "total played\n");
      for (final Long i : chunkIdPlayed.keySet()) {
        final Long info = chunkIdPlayed.get(i);
        fw.write(i + separator + info + "\n");
      }
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  /* public void printSecondInfo() { for (final Long i : secondInfo.keySet()) {
   * final SecondInfo info = secondInfo.get(i); System.out.print("second: " + i
   * + " server sent: " + info.serverUploadUtilization + " server delayed: " +
   * info.serverBitsInQueue); System.out.print(" all on average sent: " +
   * info.allUploadUtilization / info.availablenodes + " on average delayed: " +
   * info.allBitsInQueue / info.availablenodes);
   * System.out.println(" chunks played: " + info.chunksPlayed +
   * " available nodes: " + info.availablenodes); } } */
  public void printAverageDataSent() {
    for (final String prot : protocolInfo.keySet()) {
      final ProtocolInfo info = protocolInfo.get(prot);
      System.out.println("Protocol " + prot + " sent " + info.overheadMessagesSent + " overhead messages and "
          + info.duplicateChunkMessagesSent + " duplicate chunk messages out of " + info.messagesSent);
      System.out.println("Protocol " + prot + " sent " + info.overheadBytesSent + " overhead bytes and "
          + info.duplicateChunkBytesSent + " duplicate chunk bytes out of " + info.bytesSent);
    }
  }
  
  public void printOverall() {
    System.out.println("averageLag: " + averageLag);
    System.out.println("averageStartupDelay: " + averageStartupDelay);
    System.out.println("averageHopcount: " + averageHopcount);
    System.out.println("averageLatency: " + averageNodeLatency.getMean());
    System.out.println("averageLatencySD: " + averageNodeLatencySD.getMean());
    System.out.println("averageLastDelay: " + averageLastDelay);
    System.out.println("averageQuality: " + averageQuality);
    System.out.println("perferctCIpercentage: " + perfectCIpercentage);
    System.out.println("zeroCI: " + ZeroCI.getMean());
    System.out.println("zeroCItime: " + ZeroCITime.getMean());
    System.out.println("continuityIndex: " + continuityIndex.getMean());
    System.out.println("simpleContinuityIndex: " + simpleContinuityIndex.getMean());
    System.out.println("continuityIndexSD: " + continuityIndexSD.getMean());
    if (protocolInfo.containsKey("_overall")) {
      final long overhead = protocolInfo.get("_overall").overheadBytesSent;
      final long duplicate = protocolInfo.get("_overall").duplicateChunkBytesSent;
      final long all = protocolInfo.get("_overall").bytesSent;
      System.out.println("overheadBitsSent: " + overhead);
      System.out.println("totalBitsSent: " + all);
      System.out.println("overhead/vital: " + ((double) overhead) / all);
      System.out.println("duplicate/vital: " + ((double) duplicate) / all);
    }
  }
  
  public void writeOverall(final String filename) {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + filename + "." + fileExtension));
      fw.write("#overall\n");
      fw.write("averageLag" + separator + averageLag + "\n");
      fw.write("averageStartupDelay" + separator + averageStartupDelay + "\n");
      fw.write("averageHopcount" + separator + averageHopcount + "\n");
      fw.write("averageLatency" + separator + averageNodeLatency.getMean() + "\n");
      fw.write("averageLatencySD" + separator + averageNodeLatencySD.getMean() + "\n");
      fw.write("averageLastDelay" + separator + averageLastDelay + "\n");
      fw.write("averageQuality" + separator + averageQuality + "\n");
      fw.write("perferctCIpercentage" + separator + perfectCIpercentage + "\n");
      fw.write("zeroCI" + separator + ZeroCI.getMean() + "\n");
      fw.write("zeroCITime" + separator + ZeroCITime.getMean() + "\n");
      fw.write("continuityIndex" + separator + continuityIndex.getMean() + "\n");
      fw.write("simpleContinuityIndex" + separator + simpleContinuityIndex.getMean() + "\n");
      fw.write("continuityIndexSD" + separator + continuityIndexSD.getMean() + "\n");
      if (protocolInfo.containsKey("_overall")) {
        final long overhead = protocolInfo.get("_overall").overheadBytesSent;
        final long duplicate = protocolInfo.get("_overall").duplicateChunkBytesSent;
        final long all = protocolInfo.get("_overall").bytesSent;
        fw.write("overheadBitsSent" + separator + overhead + "\n");
        fw.write("totalBitsSent" + separator + all + "\n");
        fw.write("overhead/all" + separator + ((double) overhead) / all + "\n");
        fw.write("duplicate/all" + separator + ((double) duplicate) / all + "\n");
        fw.close();
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  // TODO not works for MDC
  private transient HashMap<String, Set<Long>> receivedChunks = new HashMap<String, Set<Long>>();
  
  private void handleSendLog(final SendLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    Utils.checkExistence(nodeInfo, sid.node, new NodeInfo());
    final NodeInfo sendingNodeInfo = nodeInfo.get(sid.node);
    sendingNodeInfo.messagesSent++;
    sendingNodeInfo.bytesSent += sid.messageSize;
    final String messageFullID = sid.messageTag + "-" + sid.messageType;
    Utils.checkExistence(protocolInfo, messageFullID, new ProtocolInfo());
    Utils.checkExistence(protocolInfo, sid.messageTag, new ProtocolInfo());
    Utils.checkExistence(protocolInfo, "_overall", new ProtocolInfo());
    protocolInfo.get(messageFullID).messagesSent++;
    protocolInfo.get(messageFullID).bytesSent += sid.messageSize;
    protocolInfo.get(sid.messageTag).messagesSent++;
    protocolInfo.get(sid.messageTag).bytesSent += sid.messageSize;
    protocolInfo.get("_overall").messagesSent++;
    protocolInfo.get("_overall").bytesSent += sid.messageSize;
    if (sid.isOverhead) {
      sendingNodeInfo.overheadMessagesSent++;
      sendingNodeInfo.overheadBytesSent += sid.messageSize;
      protocolInfo.get("_overall").overheadMessagesSent++;
      protocolInfo.get("_overall").overheadBytesSent += sid.messageSize;
      protocolInfo.get(messageFullID).overheadMessagesSent++;
      protocolInfo.get(messageFullID).overheadBytesSent += sid.messageSize;
      protocolInfo.get(sid.messageTag).overheadMessagesSent++;
      protocolInfo.get(sid.messageTag).overheadBytesSent += sid.messageSize;
    }
    if (sid instanceof ChunkSendLog) {
      Utils.checkExistence(receivedChunks, sid.receiveingNode, new HashSet<Long>());
      final long id = ((ChunkSendLog) sid).chunkId;
      if (receivedChunks.get(sid.receiveingNode).contains(id)) {
        sendingNodeInfo.duplicateChunkMessagesSent++;
        sendingNodeInfo.duplicateChunkBytesSent += sid.messageSize;
        protocolInfo.get("_overall").duplicateChunkMessagesSent++;
        protocolInfo.get("_overall").duplicateChunkBytesSent += sid.messageSize;
        protocolInfo.get(messageFullID).duplicateChunkMessagesSent++;
        protocolInfo.get(messageFullID).duplicateChunkBytesSent += sid.messageSize;
        protocolInfo.get(sid.messageTag).duplicateChunkMessagesSent++;
        protocolInfo.get(sid.messageTag).duplicateChunkBytesSent += sid.messageSize;
      }
      receivedChunks.get(sid.receiveingNode).add(id);
    }
  }
  
  private void calculateAverageLatencyPerOrder() {
    final double latencySum[] = new double[maxOrder + 1];
    final int latencyCount[] = new int[maxOrder + 1];
    for (int i = 0; i < maxOrder + 1; i++) {
      latencyCount[i] = 0;
      latencySum[i] = 0;
    }
    for (final String node : nodeInfo.keySet()) {
      final NodeInfo info = nodeInfo.get(node);
      for (final long chunk : info.chunkInfo.keySet()) {
        final NodeChunkInfo nci = info.chunkInfo.get(chunk);
        final double latency = nci.playLatency;
        int order = 0;
        if (node.equals(serverId)) {
          order = 0;
        } else {
          for (final int ord : nci.descToOrder.values()) {
            order += ord;
          }
          if (nci.descToOrder.size() > 0) {
            order /= nci.descToOrder.size();
          }
        }
        if (order == -1) {
          System.err.println("node " + node + " has no order for chunk " + chunk);
        } else {
          latencySum[order] += latency;
          latencyCount[order]++;
        }
      }
    }
    for (int i = 0; i < maxOrder + 1; i++) {
      final OrderInfo info = new OrderInfo();
      if (latencyCount[i] > 0) {
        info.averageLatency = latencySum[i] / latencyCount[i];
        info.count = latencyCount[i];
      } else {
        info.averageLatency = 0;
        info.count = 0;
      }
      orderInfo.put(i, info);
    }
  }
  
  public void printAverageLatencyPerOrder() {
    for (int i = 0; i < maxOrder + 1; i++) {
      final OrderInfo info = orderInfo.get(i);
      System.out.println("average latency per order " + i + ": " + info.averageLatency + ", for " + info.count + " chunks");
    }
  }
  
  private void calculateAverageOrder() {
    for (final long chunk : chunkReceiveMap.keySet()) {
      for (int i = 0; i < descriptors; ++i) {
        final Map<String/* source node */, Set<String>/* dest nodes */> chunkMap = chunkReceiveMap.get(chunk).get(i);
        if (chunkMap == null) {
          System.err.println("chunk " + chunk + ", descriptor " + i + " has no chunkReceiveMap!");
          continue;
        }
        populateChunkOrderMap(chunk, i, serverId, chunkMap, 0, new HashSet<String>());
      }
    }
    for (final String node : nodeInfo.keySet()) {
      final NodeInfo info = nodeInfo.get(node);
      Double avg = 0.0;
      int count = 0;
      for (final NodeChunkInfo chunkInfo : info.chunkInfo.values()) {
        for (final Integer order : chunkInfo.descToOrder.values()) {
          count++;
          avg += order;
          maxOrder = Math.max(maxOrder, order);
        }
      }
      avg /= count;
      if (count == 0) {
        info.chunkOrderAvg = -1;
        System.err.println("node " + node + " has no chunkInfo!");
      } else {
        info.chunkOrderAvg = avg;
      }
    }
  }
  
  private void populateChunkOrderMap(final long chunk, final int desc, final String nodeId,
      final Map<String, Set<String>> chunkMap, final int order, final HashSet<String> visited) {
    if (nodeInfo.get(nodeId) != null) {
      final NodeChunkInfo ci = nodeInfo.get(nodeId).chunkInfo.get(chunk);
      if (ci != null) {
        Utils.checkExistence(ci.descToOrder, desc, order);
        ci.descToOrder.put(desc, Math.min(order, ci.descToOrder.get(desc)));
      }
    }
    if (!chunkMap.containsKey(nodeId)) {
      return;
    }
    final Set<String> children = chunkMap.get(nodeId);
    visited.add(nodeId);
    for (final String child : children) {
      if (!visited.contains(child)) {
        populateChunkOrderMap(chunk, desc, child, chunkMap, order + 1, visited);
      }
    }
    visited.remove(nodeId);
  }
  
  private void handleAvailabilityLog(final AvailabilityLog sid) {
    if (serverId == null && sid.state == State.SERVER) {
      serverId = sid.node;
      System.out.println("server id is: " + serverId);
    }
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    final long second = sid.movieTime / 1000;
    Utils.checkExistence(secondInfo, second, new SecondInfo());
    Utils.checkExistence(secondInfo.get(second).nodesInState, sid.state, 0);
    secondInfo.get(second).nodesInState.put(sid.state, secondInfo.get(second).nodesInState.get(sid.state) + 1);
    Utils.checkExistence(nodeInfo, sid.node, new NodeInfo());
    final NodeInfo ni = nodeInfo.get(sid.node);
    if (sid.state == State.PLAYING) {
      secondInfo.get(second).latencyAvg += sid.latency;
    }
    if (ni.playSeconds == 0 && sid.played == 0.0) {
      return;
    }
    ni.playSeconds++;
    ni.ci += sid.played / sid.playbackSpeed;
  }
  
  private void handleChunkReceiveLog(final ChunkReceiveLog sid) {
    final long index = sid.index;
    Utils.checkExistence(chunkReceiveMap, index, new TreeMap<Integer, Map<String, Set<String>>>());
    final Map<Integer, Map<String, Set<String>>> descMap = chunkReceiveMap.get(index);
    for (final Integer desc : sid.descriptors) {
      Utils.checkExistence(descMap, desc, new TreeMap<String, Set<String>>());
      final Map<String, Set<String>> recMap = descMap.get(desc);
      Utils.checkExistence(recMap, sid.sourceNode, new TreeSet<String>());
      recMap.get(sid.sourceNode).add(sid.destinationNode);
    }
    if (descriptors - 1 < sid.descriptors.last()) {
      descriptors = sid.descriptors.last() + 1;
    }
  }
  
  private void calculateAverageLatency() {
    double overallAverageQuality = 0;
    int overallCount = 0;
    perfectCIpercentage = 0.0;
    for (final String node : nodeInfo.keySet()) {
      final NodeInfo info = nodeInfo.get(node);
      long firstChunk = -1;
      long count = 0;
      long delay = 0;
      double quality = 0;
      for (final long chunk : info.chunkInfo.keySet()) {
        final NodeChunkInfo chunkInfo = info.chunkInfo.get(chunk);
        if (firstChunk == -1) {
          firstChunk = chunk;
        }
        count++;
        delay += chunkInfo.playLatency;
        quality += chunkInfo.quality;
      }
      final long availablePlayTime = info.leaveTime - Math.max(info.joinTime, chunkGenerationTime.get(VideoStream.startingFrame))
          - info.startupBuffer;
      simpleContinuityIndex.addWeightedDatum(availablePlayTime, (double) info.playSeconds * info.cycle / (availablePlayTime));
      if (info.playSeconds > 0L) {
        info.ci /= info.playSeconds;
        if (node.equals(serverId)) {
          continue;
        }
        continuityIndex.addWeightedDatum(info.playSeconds, info.ci);
        if (info.ci < 1.0) {
          System.err.println("node " + node + " has <1 continuity index: " + info.ci + " play seconds: " + info.playSeconds);
        } else {
          perfectCIpercentage++;
        }
        if (info.ci > 1) {
          System.err.println("more chunks than possible for " + node + ": " + info.ci + " playSeconds: " + info.playSeconds);
        }
      }
      if (count > 0) {
        ZeroCI.addDatum(0);
        averageNodeLatency.addDatum(delay / count);
        overallAverageQuality += quality / count;
        overallCount++;
        // System.out.println("node " + node + " played "+count
        // +" chunks, from chunk: " + firstChunk+". average latency: " +
        // delay/count +" order avg: " + info.chunkOrderAvg);
      } else {
        ZeroCI.addDatum(1);
        ZeroCITime.addDatum(info.leaveTime - info.joinTime);
        System.err.println("node " + node + " didn't play any chunks! and was up from " + info.joinTime + " to " + info.leaveTime
            + " (" + (info.leaveTime - info.joinTime) + " ms)");
      }
    }
    perfectCIpercentage /= continuityIndex.getCount();
    continuityIndexSD.addDatum(continuityIndex.getStdDev());
    averageNodeLatencySD.addDatum(averageNodeLatency.getStdDev());
    averageQuality = overallAverageQuality / overallCount;
  }
  
  private void handleChunkPlayLog(final ChunkPlayLog sid) {
    if (algFilter != null && !algFilter.equals(sid.group)) {
      return;
    }
    final NodeInfo info = nodeInfo.get(sid.node);
    final NodeChunkInfo chunkInfo = new NodeChunkInfo();
    chunkInfo.playTime = sid.time;
    chunkInfo.quality = sid.quality;
    if (!chunkGenerationTime.containsKey(sid.chunkIndex)) {
      System.err.println("no generation time for " + sid.chunkIndex);
      return;
    }
    final long generationTime = chunkGenerationTime.get(sid.chunkIndex);
    chunkInfo.playLatency = sid.time - generationTime;
    info.chunkInfo.put(sid.chunkIndex, chunkInfo);
    info.lastChunkPlayed = sid.chunkIndex;
    Utils.checkExistence(chunkIdPlayed, sid.chunkIndex, 0L);
    chunkIdPlayed.put(sid.chunkIndex, chunkIdPlayed.get(sid.chunkIndex) + 1);
  }
  
  private void handleChunkGenerationLog(final ChunkGenerationLog sid) {
    chunkGenerationTime.put(sid.index, sid.generationTime);
  }
  
  public void setCSV() {
    separator = ",";
    fileExtension = "csv";
  }
  
  public void setTSV() {
    separator = "\t";
    fileExtension = "tsv";
  }
  
  public void store() {
    FileOutputStream fstream = null;
    ObjectOutputStream out = null;
    try {
      fstream = new FileOutputStream(getStoredName());
      out = new ObjectOutputStream(fstream);
      out.writeObject(this);
    } catch (final IOException e) {
      e.printStackTrace();
    } finally {
      if (fstream != null) {
        try {
          fstream.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
      if (out != null) {
        try {
          out.close();
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public String getStoredName() {
    if (algFilter == null) {
      return dirName + File.separator + "LA";
    }
    return dirName + File.separator + "LA" + algFilter;
  }
  
  static public LogAnalyzer retrieve(final String path) {
    FileInputStream fis = null;
    ObjectInputStream ois = null;
    try {
      fis = new FileInputStream(path);
      ois = new ObjectInputStream(fis);
      return (LogAnalyzer) ois.readObject();
    } catch (final Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
        if (ois != null) {
          ois.close();
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void writeConfigfile() {
    try {
      final FileWriter fw = new FileWriter(new File(dirName + File.separator + "config.xml"));
      fw.write(Common.currentConfiguration.toXml());
      fw.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  public Map<String, NodeInfo> getNodeInfo() {
    return nodeInfo;
  }
  
  public static void main(final String[] args) {
    final LogAnalyzer LA = new LogAnalyzer("collection");
    for (final String dir : args) {
      ObjectLogger.addGatheredDir(new File(dir));
    }
    // ObjectLogger.useFiles = true;
    ObjectLogger.readFromGatheredDirs();
    LA.analyze();
    LA.printAverageLatencyPerOrder();
    LA.printOverall();
    final int groups = LA.getGroupsNum();
    if (groups < 1) {
      return;
    }
    if (groups > 1) {
      for (int i = 0; i < groups; i++) {
        final LogAnalyzer groupLA = new LogAnalyzer("collection", i);
        groupLA.analyze();
        groupLA.printAverageLatencyPerOrder();
        groupLA.printOverall();
      }
    }
  }
  
  private int getGroupsNum() {
    return maxGroup + 1;
  }
  
  public double getCI() {
    return continuityIndex.getMean();
  }
}
