package modules.overlays;

import interfaces.Sizeable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestMessage;
import messages.FileMessage;
import messages.Message;
import messages.RunCmd;
import messages.StateReply;
import messages.StateRequest;
import messages.XmlMessage;
import messages.runMessage;
import modules.P2PClient;
import utils.StreamGobbler;
import utils.Utils;
import experiment.ExperimentConfiguration;
import experiment.frameworks.NodeAddress;

public class ExperimentControl extends OverlayModule<Object> {
  String version = "7";
  int runs = 5;
  int maxConcurrentProcesses = 5;
  ArrayList<Process> WorkerProcess = new ArrayList<Process>();
  Map<String, Integer> stateStat = new HashMap<String, Integer>();
  Map<String, Set<String>> stateMap = new HashMap<String, Set<String>>();
  List<String> expQueue = new LinkedList<String>();
  int timeout = 0;
  int stateTimeout = 0;
  long lastStateCheck = 0;
  long lastExpRun = 1;
  boolean idleState = false;
  
  public ExperimentControl(final P2PClient client, final Random r) {
    super(client, r);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    new Thread(new Runnable() {
      @Override public void run() {
        final String command = "";
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (command != "exit") {
          System.out.println("waiting for next command");
          try {
            commands.add(br.readLine());
          } catch (final IOException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }
  
  final ConcurrentLinkedQueue<String> commands = new ConcurrentLinkedQueue<String>();
  int connectionTimeout = 0;
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!network.isServerMode()) {
      connectionTimeout--;
      if (connectionTimeout == 0) {
        if (!isOverlayConnected()) {
          network.send(new ConnectionRequestMessage<Sizeable>(getMessageTag(), network.getAddress(), network.getServerNode()));
        }
        connectionTimeout = 30;
      }
      return;
    }
    String command = commands.poll();
    while (command != null) {
      executeCommand(command);
      command = commands.poll();
    }
    processAutoQueue();
  }
  
  @Override public void reConnect() {
    // do nothing!
  }
  
  private void processAutoQueue() {
    if (timeout > 0) {
      timeout--;
      return;
    }
    if (expQueue.isEmpty()) {
      return;
    }
    if (idleState) {
      final String exp = expQueue.remove(0);
      executeCommand("execute " + exp);
      timeout = 700;
      idleState = false;
    } else if (stateTimeout == 0) {
      executeCommand("stateRequest");
      stateTimeout = 3;
    } else {
      stateTimeout--;
      if (stateTimeout == 0) {
        for (final String key : stateMap.keySet()) {
          if (key.contains("running")) {
            timeout = 10;
            return;
          }
        }
        idleState = true;
      }
    }
  }
  
  private void executeCommand(final String command) {
    final String[] split = command.split(" ");
    if (split[0].equals("report")) {
      if (split.length > 1) {
        for (final NodeAddress n : getNeighbors()) {
          System.out.println(n.toString() + " is up");
        }
      }
      System.out.println("Total of " + getNeighbors().size() + " are up");
    } else if (split[0].equals("prepare")) {
      final String expName = split[1];
      final String serverXmlFile = expName + "Server.xml";
      final String clientXmlFile = expName + "Client.xml";
      final int currRuns = Integer.parseInt(split[2]);
      System.out.println("preparing " + expName + " for " + currRuns + " runs");
      String clientFileContents;
      String serverFileContents;
      ExperimentConfiguration expc;
      try {
        clientFileContents = Utils.readFile(clientXmlFile);
        serverFileContents = Utils.readFile(serverXmlFile);
        expc = new ExperimentConfiguration(serverFileContents);
      } catch (final Exception e) {
        e.printStackTrace();
        return;
      }
      clientFileContents = clientFileContents.replaceAll("SERVER_IP", Utils.getLocalHostName());
      clientFileContents = clientFileContents.replaceAll(expc.name, expc.name + "0");
      serverFileContents = serverFileContents.replaceAll(expc.name, expc.name + "0");
      for (int i = 1; i <= currRuns; i++) {
        clientFileContents = clientFileContents.replaceAll(expc.name + (i - 1), expc.name + i);
        serverFileContents = serverFileContents.replaceAll(expc.name + (i - 1), expc.name + i);
        network
            .send(new XmlMessage(getMessageTag(), network.getAddress(), network.getAddress(), expc.name + i, serverFileContents));
        for (final NodeAddress neighbor : getNeighbors()) {
          network.send(new XmlMessage(getMessageTag(), network.getAddress(), neighbor, expc.name + i, clientFileContents));
        }
      }
    } else if (split[0].equals("execute")) {
      final String expName = split[1];
      System.out.println("executing " + expName);
      String log = expName + "\n";
      final int clients = Integer.parseInt(split[2]);
      int j = 1;
      for (final NodeAddress neighbor : new TreeSet<NodeAddress>(getNeighbors())) {
        if (j >= clients) {
          break;
        }
        j++;
        network.send(new runMessage(getMessageTag(), network.getAddress(), neighbor, expName));
        log += neighbor + "\n";
      }
      PrintWriter writer;
      try {
        writer = new PrintWriter(expName + ".runList");
        writer.println(log);
        writer.close();
      } catch (final FileNotFoundException e) {
        e.printStackTrace();
      }
      network.send(new runMessage(getMessageTag(), network.getAddress(), network.getAddress(), expName));
    } else if (split[0].equals("stateRequest")) {
      stateMap.clear();
      stateStat.clear();
      for (final NodeAddress neighbor : getNeighbors()) {
        network.send(new StateRequest(getMessageTag(), network.getAddress(), neighbor));
      }
      network.send(new StateRequest(getMessageTag(), network.getAddress(), network.getAddress()));
    } else if (split[0].equals("state")) {
      if (split.length == 1) {
        System.out.println(stateStat);
      } else if (stateMap.containsKey(split[1])) {
        for (final String host : stateMap.get(split[1])) {
          System.out.println(host);
        }
      } else if (split[1].equals("missingFrom")) {
        try {
          final String allHostsStr = Utils.readFile(split[2]);
          final Set<String> allHosts = new HashSet<String>(Arrays.asList(allHostsStr.split("\n")));
          for (final Set<String> hosts : stateMap.values()) {
            for (final String host : hosts) {
              allHosts.remove(host.substring(0, host.lastIndexOf("-")));
            }
          }
          for (final String host : allHosts) {
            System.out.println(host);
          }
        } catch (final IOException e) {
          e.printStackTrace();
        }
      }
    } else if (split[0].equals("runCmd")) {
      stateMap.clear();
      stateStat.clear();
      final String[] cmd = Arrays.copyOfRange(split, 1, split.length);
      for (final NodeAddress neighbor : getNeighbors()) {
        network.send(new RunCmd(getMessageTag(), network.getAddress(), neighbor, cmd));
      }
      network.send(new RunCmd(getMessageTag(), network.getAddress(), network.getAddress(), cmd));
    } else if (split[0].equals("auto")) {
      try {
        final String[] lines = Utils.readFile(split[1]).split("\n");
        for (final String line : lines) {
          executeCommand("prepare " + line.split(" ")[0] + " " + runs);
          for (int i = 1; i <= runs; i++) {
            expQueue.add(line.split(" ")[0] + i + " " + line.split(" ")[1]);
            timeout++;
          }
        }
      } catch (final IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ConnectionRequestMessage) {
      addNeighbor(message.sourceId);
      network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), message.sourceId));
    } else if (message instanceof ConnectionRequestApprovedMessage<?>) {
      addNeighbor(message.sourceId);
    } else if (message instanceof XmlMessage) {
      final XmlMessage msg = (XmlMessage) message;
      try {
        final PrintWriter out = new PrintWriter(msg.name + ".xml");
        String xml = msg.contents;
        xml = xml.replaceAll("MY_HOSTNAME", Utils.getLocalHostName());
        out.print(xml);
        out.close();
      } catch (final FileNotFoundException e) {
        e.printStackTrace();
      }
    } else if (message instanceof runMessage) {
      final runMessage msg = (runMessage) message;
      final int slot = getFreeSlot();
      if (slot == -1) {
        network.send(new StateReply(getMessageTag(), network.getAddress(), message.sourceId, "ver" + version + "maxedOut"));
        return;
      }
      try {
        connectionTimeout = 600;
        final ProcessBuilder pb = new ProcessBuilder("java", "-Xmx1024m", "-jar", "./MOLStreamV1.jar", msg.xmlFileName + ".xml");
        final Process p = pb.start();
        WorkerProcess.set(slot, p);
        final StreamGobbler g1 = new StreamGobbler(p.getInputStream(), new FileOutputStream("out"));
        final StreamGobbler g2 = new StreamGobbler(p.getErrorStream(), new FileOutputStream("err"));
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              p.waitFor();
              final String zipName = Utils.getLocalHostName() + "-" + msg.xmlFileName + ".zip";
              final Process p2 = Runtime.getRuntime().exec("zip -r " + zipName + " " + msg.xmlFileName);
              WorkerProcess.set(slot, p2);
              p2.waitFor();
              g1.join(1000);
              g2.join(1000);
              // Don't end the results just yet - it clogs up the upload
              // bandwidth for the next experiment
              // node.send(new FileMessage(getMessageTag(), node.getImpl(),
              // msg.sourceId, zipName, Utils.readSmallBinaryFile(zipName)));
            } catch (final Exception e) {
              e.printStackTrace();
              network.send(new StateReply(getMessageTag(), network.getAddress(), msg.sourceId, e.toString()));
            }
          }
        }).start();
      } catch (final Exception e) {
        e.printStackTrace();
        network.send(new StateReply(getMessageTag(), network.getAddress(), msg.sourceId, e.toString()));
      }
    } else if (message instanceof StateRequest) {
      String reply = "ver" + version;
      for (int i = 0; i < WorkerProcess.size(); i++) {
        reply += "p" + i;
        if (!Utils.isTerminated(WorkerProcess.get(i))) {
          reply += "running";
        } else {
          reply += "exVal" + WorkerProcess.get(i).exitValue();
        }
      }
      network.send(new StateReply(getMessageTag(), network.getAddress(), message.sourceId, reply));
    } else if (message instanceof StateReply) {
      final StateReply msg = (StateReply) message;
      Utils.checkExistence(stateMap, msg.state, new TreeSet<String>());
      Utils.checkExistence(stateStat, msg.state, 0);
      stateStat.put(msg.state, stateStat.get(msg.state) + 1);
      stateMap.get(msg.state).add(msg.sourceId.toString());
    } else if (message instanceof FileMessage) {
      final FileMessage msg = (FileMessage) message;
      Utils.writeSmallBinaryFile(msg.bs, "resultZips/" + msg.zipName);
    } else if (message instanceof RunCmd) {
      final RunCmd msg = (RunCmd) message;
      if (msg.cmd[0].equals("stop")) {
        System.exit(0);
      }
      try {
        if (msg.cmd[0].equals("kill")) {
          if (msg.cmd.length > 1) {
            final int p = Integer.parseInt(msg.cmd[1]);
            if (p < WorkerProcess.size()) {
              WorkerProcess.remove(p).destroy();
            }
          } else {
            while (WorkerProcess.size() > 0) {
              WorkerProcess.remove(0).destroy();
            }
          }
          return;
        }
        if (msg.cmd[0].equals("cleanZips")) {
          String retVal = "noZips";
          for (final File child : new File(".").listFiles()) {
            if (child.getName().endsWith(".zip")) {
              child.delete();
              retVal = "deletedZips";
            }
          }
          network.send(new StateReply(getMessageTag(), network.getAddress(), message.sourceId, retVal));
          return;
        }
        final int slot = getFreeSlot();
        if (slot == -1) {
          network.send(new StateReply(getMessageTag(), network.getAddress(), message.sourceId, "ver" + version + "maxedOut"));
          return;
        }
        final ProcessBuilder pb = new ProcessBuilder(msg.cmd);
        pb.redirectErrorStream(true);
        final Process p = pb.start();
        final StreamGobbler p1 = new StreamGobbler(p.getInputStream(), new FileOutputStream("cmdout"));
        WorkerProcess.set(slot, p);
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              p.waitFor();
              p1.join(1000);
              network.send(new StateReply(getMessageTag(), network.getAddress(), msg.sourceId, Utils.readFile("cmdout")));
            } catch (final Exception e) {
              e.printStackTrace();
              network.send(new StateReply(getMessageTag(), network.getAddress(), msg.sourceId, e.toString()));
            }
          }
        }).start();
      } catch (final Exception e) {
        network.send(new StateReply(getMessageTag(), network.getAddress(), msg.sourceId, e.toString()));
      }
    }
  }
  
  private int getFreeSlot() {
    for (int i = 0; i < WorkerProcess.size(); i++) {
      if (Utils.isTerminated(WorkerProcess.get(i))) {
        return i;
      }
    }
    if (WorkerProcess.size() < maxConcurrentProcesses) {
      WorkerProcess.add(null);
      return WorkerProcess.size() - 1;
    }
    return -1;
  }
}
