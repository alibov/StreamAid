package experiment;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import logging.TextLogger;
import modules.P2PClient;
import modules.network.ip.FailureDetector;
import modules.network.ip.PingFailureDetector;
import modules.network.ip.PingMessageFailureDetector;
import modules.network.ip.YesManFailureDetector;
import modules.overlays.OverlayModule;
import modules.player.PlayerModule;
import modules.streaming.StreamingModule;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import utils.Common;
import utils.RandomCollection;
import utils.Utils;
import utils.distributions.ConstantDistribution;
import utils.distributions.Distribution;
import utils.distributions.ExponentialDistribution;
import utils.distributions.InfiniteDistribution;
import utils.distributions.LogNormalDistribution;
import utils.distributions.MovieTimeDistribution;
import utils.distributions.NormalDistribution;
import utils.distributions.ParetoDistribution;
import utils.distributions.UniformDistribution;
import utils.distributions.WeibullDistribution;
import entites.NodeAvailability;
import experiment.ChurnModel.Type;
import experiment.frameworks.NetworkFramework;
import experiment.frameworks.NetworkFramework.Role;
import experiment.frameworks.NodeAddress;
import experiment.frameworks.P2PFramework;
import experiment.frameworks.PeerSimFramework;
import experiment.runner.LocalRunManager;
import experiment.runner.RunManager;
import experiment.runner.SingleNodeRunManager;

/**
 * class storing configuration for a single experiment
 *
 * @author Alexander Libov
 */
public class ExperimentConfiguration implements Serializable {
  private static final long serialVersionUID = -4898621974479705358L;
  public Long debugSeed = null;
  public final String name;
  public final int nodes;
  public final int playbackSeconds;
  public final int runs;
  public final int messageHeaderSize;
  public final Distribution uploadBandwidthDistribution;
  public final int bitRate;
  public final long serverUploadBandwidth;
  public final boolean waitQueueLimit;
  public final int descriptions;
  public final long cycleLength;
  public final ChurnModel churnModel;
  public Random uploadBandwidthRandom;
  public final P2PFramework framework;
  public final boolean sendPartialMessage;
  private final Integer groupStartingNumber;
  private static String defaultName = "defaultExperiment";
  private static boolean defaultEnableTextLogger = false;
  private static boolean defaultWaitQueueLimit = false;
  private static int defaultRuns = 1;
  private static Integer defaultAlgStartingNumber = 0;
  private static int defaultBitRate = 200;
  private static int defaultPlaybackSeconds = 250;
  private static int defaultHeaderSize = 64;
  private static long defaultServerBandwidth = Long.MAX_VALUE;
  private static int defaultDesctiptions = 1;
  private static long defaultCycleLength = 1000;
  private static P2PFramework defaultFramework = new PeerSimFramework(0.0, 200, 400);
  private static FailureDetector defaultFailureDetector = new PingMessageFailureDetector();
  public static boolean defaultSendPartialMessage;
  ArrayList<GetStreamingMod> getStreamingMod = new ArrayList<ExperimentConfiguration.GetStreamingMod>();
  GetPlayerMod getPlayerMod;
  ArrayList<Integer> bounds = new ArrayList<Integer>();
  ArrayList<String> streamingAlgorithmXml = new ArrayList<String>();
  // no churn by default
  String uniqueName = Thread.currentThread().getId() + "t" + String.valueOf(new Date().getTime());
  private static ChurnModel defaultChurnModel = new ChurnModel(Type.none);
  // mean data taken from netindex.com upload index
  // private static Distribution defaultUploadBandwidthDistribution = new
  // ConstantDistribution(5560000);
  private static Distribution defaultUploadBandwidthDistribution = new InfiniteDistribution();
  public List<Long> seeds = new LinkedList<Long>();
  
  public ExperimentConfiguration(final Document doc) throws IOException {
    final NodeList list = doc.getFirstChild().getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node n = list.item(i);
      final String nodeName = n.getNodeName();
      if (nodeName.equals("name")) {
        defaultName = ((Attr) n.getAttributes().getNamedItem("value")).getValue();
      } else if (nodeName.equals("playbackSeconds")) {
        defaultPlaybackSeconds = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("churnModel")) {
        defaultChurnModel = initChurnModel(n);
      } else if (nodeName.equals("uploadBandwidthDistribution")) {
        defaultUploadBandwidthDistribution = initDistribution(n.getFirstChild().getNextSibling());
      } else if (nodeName.equals("runs")) {
        defaultRuns = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("seed")) {
        Common.seed = Long.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("bitrate")) {
        defaultBitRate = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("serverUploadBandwidth")) {
        defaultServerBandwidth = Long.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("messageHeaderSize")) {
        defaultHeaderSize = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("descriptions")) {
        defaultDesctiptions = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("cycleLength")) {
        defaultCycleLength = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("streamingModule")) {
        getStreamingMod.add(StreamingModuleFactory.initStreamingModule(n));
        streamingAlgorithmXml.add(XMLutils.nodeToString(n));
        int size = 1; // if no size specified => treat as 1
        if (n.getAttributes().getNamedItem("size") != null) {
          size = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("size")).getValue());
        }
        if (bounds.isEmpty()) {
          bounds.add(size);
        } else {
          bounds.add(bounds.get(bounds.size() - 1) + size);
        }
      } else if (nodeName.equals("playerModule")) {
        getPlayerMod = initPlayerModule(n);
      } else if (nodeName.equals("enableTextLogger")) {
        defaultEnableTextLogger = true;
      } else if (nodeName.equals("debugSeed")) {
        debugSeed = Long.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("framework")) {
        defaultFramework = initFramework(n);
      } else if (nodeName.equals("algStartingNumber")) {
        defaultAlgStartingNumber = Integer.valueOf(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      } else if (nodeName.equals("enableWaitQueueLimit")) {
        defaultWaitQueueLimit = true;
      } else if (nodeName.equals("#text") || nodeName.equals("#comment")) {
        // do nothing
      } else {
        throw new IllegalStateException("unrecognized element " + nodeName);
      }
    }
    if (getStreamingMod == null) {
      throw new IllegalStateException("configuration must provide an streamingAlgorithm");
    }
    if (defaultChurnModel.type.equals(ChurnModel.Type.availabilityFile)) {
      nodes = 1;
    } else {
      nodes = bounds.get(bounds.size() - 1);
    }
    name = defaultName;
    runs = debugSeed == null ? defaultRuns : 1;
    bitRate = defaultBitRate;
    playbackSeconds = defaultPlaybackSeconds;
    uploadBandwidthDistribution = defaultUploadBandwidthDistribution;
    messageHeaderSize = defaultHeaderSize;
    serverUploadBandwidth = defaultServerBandwidth;
    descriptions = defaultDesctiptions;
    cycleLength = defaultCycleLength;
    churnModel = defaultChurnModel;
    framework = defaultFramework;
    waitQueueLimit = defaultWaitQueueLimit;
    TextLogger.disabled = !defaultEnableTextLogger;
    groupStartingNumber = defaultAlgStartingNumber;
    sendPartialMessage = defaultSendPartialMessage;
    if (nodes > framework.getMaxNodes()) {
      throw new RuntimeException("more nodes than framework allows");
    }
    setSeeds();
  }
  
  private static ChurnModel initChurnModel(final Node n) throws IOException {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to initChurnModel: " + n.getNodeName());
    }
    final Type type = ChurnModel.Type.valueOf(((Attr) n.getAttributes().getNamedItem("type")).getValue());
    final ChurnModel retVal = new ChurnModel(type);
    switch (type) {
      case none: // do nothing
        break;
      case sessionLengthAddOnFailure:
        setSessionLength(n, retVal);
        break;
      case sessionLengthOffLength:
        setSessionLength(n, retVal);
        setOffLength(n, retVal);
        break;
      case sessionLengthInterArrival:
        setSessionLength(n, retVal);
        setInterArrival(n, retVal);
        break;
      case availabilityFile:
        parseAvailabilityFile(n, retVal);
        break;
      case eventBased:
        parseEvents(n, retVal);
        break;
      default:
        throw new RuntimeException("unhandled case for " + type);
    }
    return retVal;
  }
  
  private static void parseEvents(final Node n, final ChurnModel cm) {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to parseEvents: " + n.getNodeName());
    }
    final NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);
      if (child.getNodeName().equals("event")) {
        final experiment.ChurnModel.Event.Type type = ChurnModel.Event.Type.valueOf(((Attr) child.getAttributes().getNamedItem(
            "type")).getValue());
        final int amount = Integer.parseInt(((Attr) child.getAttributes().getNamedItem("amount")).getValue());
        final int time = Integer.parseInt(((Attr) child.getAttributes().getNamedItem("time")).getValue());
        cm.addEvent(new experiment.ChurnModel.Event(type, amount, time));
      }
    }
  }
  
  private static void parseAvailabilityFile(final Node n, final ChurnModel cm) throws IOException {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to setOffLength: " + n.getNodeName());
    }
    final NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);
      if (child.getNodeName().equals("availabilityFile")) {
        final String fileName = ((Attr) child.getAttributes().getNamedItem("value")).getValue();
        final File file = new File(fileName);
        cm.setAvailabilityFile(fileName);
        parseAvailabilityFile(file, cm);
      }
    }
  }
  
  private static void setInterArrival(final Node n, final ChurnModel cm) {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to setOffLength: " + n.getNodeName());
    }
    final NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);
      if (child.getNodeName().equals("interArrivalDistribution")) {
        cm.setInterArrivalDistribution(initDistribution(XMLutils.findFirstChild(child, "distribution")));
      }
    }
  }
  
  private static void setOffLength(final Node n, final ChurnModel cm) {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to setOffLength: " + n.getNodeName());
    }
    final NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);
      if (child.getNodeName().equals("offLengthDistribution")) {
        cm.setOffLengthDistribution(initDistribution(XMLutils.findFirstChild(child, "distribution")));
      }
    }
  }
  
  private static void setSessionLength(final Node n, final ChurnModel cm) {
    if (!n.getNodeName().equals("churnModel")) {
      throw new IllegalStateException("illegal node passed to setSessionLength: " + n.getNodeName());
    }
    final NodeList list = n.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      final Node child = list.item(i);
      if (child.getNodeName().equals("sessionLengthDistribution")) {
        cm.setSessionLengthDistribution(initDistribution(XMLutils.findFirstChild(child, "distribution")));
      }
    }
  }
  
  private static P2PFramework initFramework(final Node n) throws UnknownHostException {
    if (!n.getNodeName().equals("framework")) {
      throw new IllegalStateException("illegal node passed to initFramework: " + n.getNodeName());
    }
    P2PFramework retVal = null;
    final String name = ((Attr) n.getAttributes().getNamedItem("value")).getValue();
    if (name.equals(PeerSimFramework.class.getSimpleName())) {
      final double dropRate = Double.parseDouble(XMLutils.getAttribute(n, "dropRate", "0"));
      final long minDelay = Long.parseLong(XMLutils.getAttribute(n, "minDelay", "200"));
      final long maxDelay = Long.parseLong(XMLutils.getAttribute(n, "maxDelay", "400"));
      defaultSendPartialMessage = n.getAttributes().getNamedItem("sendPartialMessage") != null;
      retVal = new PeerSimFramework(dropRate, minDelay, maxDelay);
    } else if (name.equals(NetworkFramework.class.getSimpleName())) {
      final Role role = Role.valueOf(((Attr) n.getAttributes().getNamedItem("role")).getValue());
      final int port = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("port")).getValue());
      InetSocketAddress address = null;
      if (role.equals(Role.server)) {
        address = new InetSocketAddress(InetAddress.getLocalHost(), port);
      }
      if (n.hasChildNodes()) {
        address = getAddress(getNamedChildren(n, "address").get(0));
      }
      if (address == null) {
        throw new IllegalStateException("client must get server address");
      }
      FailureDetector fd = defaultFailureDetector;
      if (n.hasChildNodes()) {
        final List<Node> fds = getNamedChildren(n, "failureDetector");
        if (!fds.isEmpty()) {
          fd = getFailureDetector(fds.get(0));
        }
      }
      retVal = new NetworkFramework(role, port, address, fd);
    } else {
      throw new RuntimeException("no framework " + name + " defined!");
    }
    return retVal;
  }
  
  // TODO document
  private static FailureDetector getFailureDetector(final Node n) {
    if (!n.getNodeName().equals("failureDetector")) {
      throw new IllegalStateException("illegal node passed to getFailureDetector: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("type")).getValue();
    if (alg.equals(PingMessageFailureDetector.class.getSimpleName())) {
      return new PingMessageFailureDetector();
    }
    if (alg.equals(PingFailureDetector.class.getSimpleName())) {
      return new PingFailureDetector();
    }
    if (alg.equals(YesManFailureDetector.class.getSimpleName())) {
      return new YesManFailureDetector();
    }
    throw new IllegalStateException("no failureDetector " + alg + " defined!");
  }
  
  private static InetSocketAddress getAddress(final Node n) throws UnknownHostException {
    if (!n.getNodeName().equals("address")) {
      throw new IllegalStateException("illegal node passed to getAddress: " + n.getNodeName());
    }
    final int port = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("port")).getValue());
    if (n.getAttributes().getNamedItem("hostname") != null) {
      final InetAddress ip = InetAddress.getByName((((Attr) n.getAttributes().getNamedItem("hostname")).getValue()));
      return new InetSocketAddress(ip, port);
    }
    throw new RuntimeException("address must have the hostname attribute");
  }
  
  private static void parseAvailabilityFile(final File file, final ChurnModel cm) throws IOException {
    if (!file.exists()) {
      throw new IllegalArgumentException("no availability file " + file);
    }
    final BufferedReader br = new BufferedReader(new FileReader(file));
    String line;
    long maxTime = 0;
    final TreeMap<Integer, NodeAvailability> nodeAvailability = new TreeMap<Integer, NodeAvailability>();
    int counter = 1; // 0 is the server
    while ((line = br.readLine()) != null) {
      final String[] split = line.split(" ");
      final long lastTime = Long.parseLong(split[2]);
      nodeAvailability.put(counter, new NodeAvailability(Long.parseLong(split[1]), lastTime));
      maxTime = Math.max(maxTime, lastTime);
      counter++;
    }
    defaultPlaybackSeconds = (int) (maxTime / 1000);
    cm.setNodeAvailability(nodeAvailability);
    br.close();
  }
  
  private static Distribution initDistribution(final Node n) {
    if (!n.getNodeName().equals("distribution")) {
      throw new IllegalStateException("illegal node passed to initDistribution: " + n.getNodeName());
    }
    final String alg = ((Attr) n.getAttributes().getNamedItem("type")).getValue();
    if (alg.equals(LogNormalDistribution.class.getSimpleName())) {
      final double mean = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("mean")).getValue());
      final double variance = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("variance")).getValue());
      return new LogNormalDistribution(mean, variance);
    } else if (alg.equals(ParetoDistribution.class.getSimpleName())) {
      final double xm = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("Xm")).getValue());
      final double alpha = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("alpha")).getValue());
      return new ParetoDistribution(xm, alpha);
    } else if (alg.equals(NormalDistribution.class.getSimpleName())) {
      final double mean = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("mean")).getValue());
      final double variance = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("variance")).getValue());
      return new NormalDistribution(mean, variance);
    } else if (alg.equals(UniformDistribution.class.getSimpleName())) {
      final double min = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("min")).getValue());
      final double max = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("max")).getValue());
      return new UniformDistribution(min, max);
    } else if (alg.equals(WeibullDistribution.class.getSimpleName())) {
      final double lambda = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("lambda")).getValue());
      final double k = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("k")).getValue());
      return new WeibullDistribution(lambda, k);
    } else if (alg.equals(ConstantDistribution.class.getSimpleName())) {
      final double value = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("value")).getValue());
      return new ConstantDistribution(value);
    } else if (alg.equals(ExponentialDistribution.class.getSimpleName())) {
      final double mean = Double.parseDouble(((Attr) n.getAttributes().getNamedItem("mean")).getValue());
      return new ExponentialDistribution(mean);
    } else if (alg.equals(InfiniteDistribution.class.getSimpleName())) {
      return new InfiniteDistribution();
    } else if (alg.equals(MovieTimeDistribution.class.getSimpleName())) {
      final ArrayList<Distribution> distros = new ArrayList<Distribution>();
      for (int i = 0; i < n.getChildNodes().getLength(); ++i) {
        final Node c = n.getChildNodes().item(i);
        if (!c.getNodeName().equals("distribution")) {
          continue;
        }
        final Distribution d = initDistribution(c);
        if (((Attr) c.getAttributes().getNamedItem("from")) != null) {
          d.fromTime = Integer.parseInt(((Attr) c.getAttributes().getNamedItem("from")).getValue());
        }
        distros.add(d);
      }
      return new MovieTimeDistribution(distros);
    } else {
      throw new IllegalStateException("unrecognized distribution: " + alg);
    }
  }
  
  private static GetPlayerMod initPlayerModule(final Node n) {
    if (!n.getNodeName().equals("playerModule")) {
      throw new IllegalStateException("illegal node passed to initPlayerModule: " + n.getNodeName());
    }
    final boolean waitIfNoChunk = n.getAttributes().getNamedItem("waitIfNoChunk") != null;
    final boolean bufferFromFirstChunk = n.getAttributes().getNamedItem("bufferFromFirstChunk") != null;
    final int serverStartup = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("serverStartup")).getValue());
    final int startupBuffering = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("startupBuffering")).getValue());
    final int streamWindowSize = Integer.parseInt(((Attr) n.getAttributes().getNamedItem("streamWindowSize")).getValue());
    return new GetPlayerMod() {
      @Override public String getName() {
        return (waitIfNoChunk ? "w" : "") + (bufferFromFirstChunk ? "b" : "") + "s" + serverStartup + "sb" + startupBuffering;
      }
      
      @Override public PlayerModule getAlg(final P2PClient client) {
        return new PlayerModule(client, waitIfNoChunk, bufferFromFirstChunk, serverStartup, startupBuffering, streamWindowSize);
      }
    };
  }
  
  public ExperimentConfiguration(final File confFile) throws ParserConfigurationException, SAXException, IOException {
    this(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(confFile));
  }
  
  public ExperimentConfiguration(final InputStream confXmlString) throws ParserConfigurationException, SAXException, IOException {
    this(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(confXmlString));
  }
  
  public ExperimentConfiguration(final String confXmlString) throws ParserConfigurationException, SAXException, IOException {
    this(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(confXmlString.getBytes())));
  }
  
  @Override public String toString() {
    return uniqueName + "size" + nodes + "length" + playbackSeconds + "br" + bitRate + getStreamingMod.get(0).getName() + "g"
        + getStreamingMod.size();
  }
  
  public String toXml() {
    final StringBuilder sb = new StringBuilder();
    sb.append("<experiment>\n");
    sb.append("\t<name value=\"" + name + "\"/>\n");
    sb.append("\t<playbackSeconds value=\"" + playbackSeconds + "\"/>\n");
    sb.append("\t<runs value=\"" + runs + "\"/>\n");
    sb.append("\t<uploadBandwidthDistribution>\n");
    sb.append("\t\t" + uploadBandwidthDistribution.toXml());
    sb.append("\t</uploadBandwidthDistribution>\n");
    sb.append("\t<serverUploadBandwidth value=\"" + serverUploadBandwidth + "\"/>\n");
    sb.append("\t<messageHeaderSize value=\"" + messageHeaderSize + "\"/>\n");
    sb.append("\t<seed value=\"" + Common.seed + "\"/>\n");
    sb.append("\t<descriptions value=\"" + descriptions + "\"/>\n");
    sb.append("\t<cycleLength value=\"" + cycleLength + "\"/>\n");
    sb.append(churnModel.toXml("\t") + "\n");
    sb.append(getPlayerMod.getAlg(null).toXml("\t") + "\n");
    sb.append(framework.toXml("\t") + "\n");
    sb.append("\t<bitrate value=\"" + bitRate + "\"/> <!-- " + bitRate / 1000 + " kbps --> \n");
    for (int i = 0; i < streamingAlgorithmXml.size(); ++i) {
      sb.append("\t" + streamingAlgorithmXml.get(i));
    }
    sb.append("</experiment>\n");
    return sb.toString();
  }
  
  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + bitRate;
    result = prime * result + ((churnModel == null) ? 0 : churnModel.hashCode());
    result = prime * result + (int) (cycleLength ^ (cycleLength >>> 32));
    result = prime * result + ((debugSeed == null) ? 0 : debugSeed.hashCode());
    result = prime * result + descriptions;
    result = prime * result + ((experimentRandom == null) ? 0 : experimentRandom.hashCode());
    result = prime * result + ((framework == null) ? 0 : framework.hashCode());
    result = prime * result + ((getStreamingMod == null) ? 0 : getStreamingMod.hashCode());
    result = prime * result + messageHeaderSize;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + nodes;
    result = prime * result + playbackSeconds;
    result = prime * result + runs;
    result = prime * result + ((seeds == null) ? 0 : seeds.hashCode());
    result = prime * result + (int) (serverUploadBandwidth ^ (serverUploadBandwidth >>> 32));
    result = prime * result + ((streamingAlgorithmXml == null) ? 0 : streamingAlgorithmXml.hashCode());
    result = prime * result + ((uploadBandwidthDistribution == null) ? 0 : uploadBandwidthDistribution.hashCode());
    return result;
  }
  
  @Override public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ExperimentConfiguration other = (ExperimentConfiguration) obj;
    if (bitRate != other.bitRate) {
      return false;
    }
    if (churnModel == null) {
      if (other.churnModel != null) {
        return false;
      }
    } else if (!churnModel.equals(other.churnModel)) {
      return false;
    }
    if (cycleLength != other.cycleLength) {
      return false;
    }
    if (debugSeed == null) {
      if (other.debugSeed != null) {
        return false;
      }
    } else if (!debugSeed.equals(other.debugSeed)) {
      return false;
    }
    if (descriptions != other.descriptions) {
      return false;
    }
    if (experimentRandom == null) {
      if (other.experimentRandom != null) {
        return false;
      }
    }
    if (framework == null) {
      if (other.framework != null) {
        return false;
      }
    } else if (!framework.equals(other.framework)) {
      return false;
    }
    if (getStreamingMod == null) {
      if (other.getStreamingMod != null) {
        return false;
      }
    }
    if ((getStreamingMod.size() != other.getStreamingMod.size())) {
      return false;
    }
    for (int i = 0; i < getStreamingMod.size(); i++) {
      if (!getStreamingMod.get(i).getName().equals(other.getStreamingMod.get(i).getName())) {
        return false;
      }
    }
    if (messageHeaderSize != other.messageHeaderSize) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (nodes != other.nodes) {
      return false;
    }
    if (playbackSeconds != other.playbackSeconds) {
      return false;
    }
    if (runs != other.runs) {
      return false;
    }
    if (seeds == null) {
      if (other.seeds != null) {
        return false;
      }
    } else if (!seeds.equals(other.seeds)) {
      return false;
    }
    if (serverUploadBandwidth != other.serverUploadBandwidth) {
      return false;
    }
    if (streamingAlgorithmXml == null) {
      if (other.streamingAlgorithmXml != null) {
        return false;
      }
    } else if (!streamingAlgorithmXml.equals(other.streamingAlgorithmXml)) {
      return false;
    }
    if (uploadBandwidthDistribution == null) {
      if (other.uploadBandwidthDistribution != null) {
        return false;
      }
    } else if (!uploadBandwidthDistribution.equals(other.uploadBandwidthDistribution)) {
      return false;
    }
    return true;
  }
  
  public Random experimentRandom;
  
  private void setSeeds() {
    final Random r = new Random(Common.seed);
    for (int i = 0; i < runs; ++i) {
      seeds.add(r.nextLong() & (1L << 48) - 1);
    }
  }
  
  public interface GetPlayerMod {
    public PlayerModule getAlg(P2PClient client);
    
    public String getName();
  }
  
  public interface GetStreamingMod {
    public StreamingModule getAlg(P2PClient client);
    
    public String getName();
    
    @Override public int hashCode();
    
    @Override public boolean equals(Object obj);
  }
  
  public interface GetOverlayMod {
    public OverlayModule<?> getAlg(P2PClient client);
    
    public String getName();
  }
  
  private int counter = 0;
  private int currAlg = 0;
  private final Map<String, Integer> IDtoGroupIndex = new HashMap<String, Integer>();
  private final Map<Integer, Set<NodeAddress>> grouptoIDs = new HashMap<Integer, Set<NodeAddress>>();
  private final Queue<Integer> nextAlgIndex = new LinkedList<Integer>();
  
  public void init(final long churnSeed, final long upbseed) {
    counter = 0;
    currAlg = 0;
    IDtoGroupIndex.clear();
    nextAlgIndex.clear();
    grouptoIDs.clear();
    churnModel.setRandom(new Random(churnSeed));
    uploadBandwidthRandom = new Random(upbseed);
  }
  
  public void nodeRemoved(final String nodeID) {
    nextAlgIndex.add(IDtoGroupIndex.remove(nodeID));
  }
  
  public int getNodeGroup(final String ID) {
    if (IDtoGroupIndex.get(ID) == null) {
      return -1;
    }
    return IDtoGroupIndex.get(ID) + groupStartingNumber;
  }
  
  public Set<NodeAddress> getGroupMemebers(final int alg) {
    return grouptoIDs.get(alg);
  }
  
  public StreamingModule getStreamingModule(final P2PClient client) {
    StreamingModule ncl = null;
    try {
      int group = -1;
      if (counter < nodes) {
        while (counter > bounds.get(currAlg) && currAlg < bounds.size()) {
          currAlg++;
        }
        counter++;
        group = currAlg;
      } else if (!nextAlgIndex.isEmpty()) {
        group = nextAlgIndex.poll();
      } else { // no more nodes to add, adding according to bounds
        final RandomCollection<Integer> rc = new RandomCollection<Integer>(experimentRandom);
        for (int i = 0; i < bounds.size(); i++) {
          rc.add(bounds.get(i), i);
        }
        group = rc.next();
      }
      if (client.isServerMode()) {
        group = 0; // first algorithm always for the server
      }
      TextLogger.log(client.network.getAddress(), "using alg: " + group + "\n");
      ncl = getStreamingMod.get(group).getAlg(client);
      IDtoGroupIndex.put(client.network.getAddress().toString(), group);
      Utils.checkExistence(grouptoIDs, group, new TreeSet<NodeAddress>());
      grouptoIDs.get(group).add(client.network.getAddress());
    } catch (final Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    return ncl;
  }
  
  public PlayerModule getPlayerModule(final P2PClient p2pClient) {
    return getPlayerMod.getAlg(p2pClient);
  }
  
  public static void setDefaultName(final String defaultName) {
    ExperimentConfiguration.defaultName = defaultName;
  }
  
  public static void setDefaultRuns(final int defaultRuns) {
    ExperimentConfiguration.defaultRuns = defaultRuns;
  }
  
  public static void setDefaultBitRate(final int defaultBitRate) {
    ExperimentConfiguration.defaultBitRate = defaultBitRate;
  }
  
  public static void setDefaultPlaybackSeconds(final int defaultPlaybackSeconds) {
    ExperimentConfiguration.defaultPlaybackSeconds = defaultPlaybackSeconds;
  }
  
  public static void setDefaultUploadBandwidthDistribution(final Distribution defaultUploadBandwidthDistribution) {
    ExperimentConfiguration.defaultUploadBandwidthDistribution = defaultUploadBandwidthDistribution;
  }
  
  public static void setDefaultDesctiptions(final int defaultDesctiptions) {
    ExperimentConfiguration.defaultDesctiptions = defaultDesctiptions;
  }
  
  public static void setDefaultChurnModel(final ChurnModel cm) {
    defaultChurnModel = cm;
  }
  
  public static void setDefaultFramework(final P2PFramework defaultFramework) {
    ExperimentConfiguration.defaultFramework = defaultFramework;
  }
  
  public static void setDefaultServerBandwidth(final long defaultServerBandwidth) {
    ExperimentConfiguration.defaultServerBandwidth = defaultServerBandwidth;
  }
  
  public RunManager getRunManager() {
    return framework.isSimulator() ? new LocalRunManager() : new SingleNodeRunManager();
  }
  
  private static List<Node> getNamedChildren(final Node n, final String name) {
    final List<Node> retVal = new LinkedList<Node>();
    for (int i = 0; i < n.getChildNodes().getLength(); i++) {
      if (n.getChildNodes().item(i).getNodeName().equals(name)) {
        retVal.add(n.getChildNodes().item(i));
      }
    }
    return retVal;
  }
  
  public int getGroupNumber() {
    return getStreamingMod.size();
  }
}
