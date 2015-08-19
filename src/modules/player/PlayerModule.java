package modules.player;

import java.util.LinkedList;
import java.util.List;

import logging.ObjectLogger;
import logging.TextLogger;
import logging.logObjects.AvailabilityLog;
import logging.logObjects.AvailabilityLog.State;
import logging.logObjects.ChunkPlayLog;
import modules.P2PClient;
import utils.Utils;

public class PlayerModule {
  private static ObjectLogger<ChunkPlayLog> chunkPlayLogger = ObjectLogger.getLogger("chunkPlay");
  private static ObjectLogger<AvailabilityLog> availabilityLogger = ObjectLogger.getLogger("availLog");
  private VideoStream vs;
  private static int lagThreshold = 10;
  private int consecutiveLagSeconds = 0;
  private int playSeconds = 0;
  private double localCI = 0.0;
  private double totalLatency = 0.0;
  private int totalChunksPlayed = 0;
  private SkipPolicy vsSkipPolicy = null;
  private final List<StreamListener> listeners = new LinkedList<StreamListener>();
  private int delay = 0;
  private final P2PClient client;
  public final boolean waitIfNoChunk;
  public final boolean bufferFromFirstChunk;
  public final int serverStartup;
  public final int startupBuffering;
  public final int streamWindowSize;
  public int currentConfNumber;
  
  public PlayerModule(final P2PClient client, final boolean waitIfNoChunk, final boolean bufferFromFirstChunk,
      final int serverStartup, final int startupBuffering, final int streamWindowSize) {
    this.client = client;
    this.waitIfNoChunk = waitIfNoChunk;
    this.bufferFromFirstChunk = bufferFromFirstChunk;
    this.serverStartup = serverStartup;
    this.startupBuffering = startupBuffering;
    this.streamWindowSize = streamWindowSize;
  }
  
  public void beforeStreamingCycle() {
    if (vs != null) {
      vs.nextCycle();
    }
  }
  
  public void afterStreamingCycle() {
    double chunksPlayed = 0.0;
    AvailabilityLog.State state;
    double latency = -1;
    if (vs != null && !vs.isBuffering() && Utils.getMovieTime() >= 0 && delay == 0) {
      playSeconds++;
      final long chunkToBePlayed = vs.windowOffset;
      chunksPlayed = vs.playChunk();
      localCI += chunksPlayed / vs.playSpeed;
      for (final ChunkPlayLog CPL : vs.chunkPlayed) {
        chunkPlayLogger.logObject(new ChunkPlayLog(CPL.chunkIndex, client.network.getAddress().toString(), CPL.time, CPL.quality));
        TextLogger.log(client.network.getAddress(), "played " + CPL.chunkIndex + "\n");
        latency = CPL.time - CPL.chunkIndex * 1000;
        totalLatency += latency;
        totalChunksPlayed++;
      }
      vs.chunkPlayed.clear();
      if (chunksPlayed < vs.playSpeed) {
        consecutiveLagSeconds++;
        TextLogger.log(client.network.getAddress(), "couldn't play chunk " + chunkToBePlayed + "\n");
        state = State.LAGGING;
      } else {
        consecutiveLagSeconds = 0;
        state = State.PLAYING;
      }
    } else if (vs == null && Utils.getMovieTime() > 0) {
      consecutiveLagSeconds++;
      state = State.NEW;
    } else if ((vs != null && vs.isBuffering()) || delay > 0) {
      consecutiveLagSeconds = 0;
      state = State.BUFFERING;
    } else if (vs == null && Utils.getMovieTime() <= 0) {
      state = State.NEW;
    } else if (client.isServerMode()) {
      state = State.SERVER;
    } else {
      throw new IllegalStateException();
    }
    if (consecutiveLagSeconds > lagThreshold) {
      client.handleConsecutiveLag();
      consecutiveLagSeconds = 0;
    }
    if (client.isServerMode()) {
      state = State.SERVER;
    }
    availabilityLogger.logObject(new AvailabilityLog(client.network.getAddress().toString(), state, vs == null ? 0L : vs.playSpeed,
        chunksPlayed, latency));
    if (delay > 0) {
      delay--;
    }
  }
  
  public VideoStream getVs() {
    return vs;
  }
  
  public void setServerMode() {
    vs = new ServerVideoStream(serverStartup, streamWindowSize, waitIfNoChunk, startupBuffering);
  }
  
  public void initVS(final long startingFrame) {
    if (vs != null) {
      throw new RuntimeException("initVS called multiple times!");
    }
    TextLogger.log(client.network.getAddress(), "init VS called with " + startingFrame + "\n");
    vs = new VideoStream(startingFrame, streamWindowSize, waitIfNoChunk, startupBuffering);
    vs.addListeners(listeners);
    if (vsSkipPolicy != null) {
      vs.setSkipPolicy(vsSkipPolicy);
    }
  }
  
  public void setVSskipPolicy(final SkipPolicy policy) {
    if (vs != null) {
      vs.setSkipPolicy(policy);
      return;
    }
    vsSkipPolicy = policy;
  }
  
  public void addVSlistener(final StreamListener listener) {
    if (vs != null) {
      vs.addListener(listener);
      return;
    }
    listeners.add(listener);
  }
  
  public void removeVSlistener(final StreamListener listener) {
    listeners.remove(listener);
    if (vs != null) {
      vs.removeListener(listener);
    }
  }
  
  public double getCI() {
    return localCI / playSeconds;
  }
  
  public double getLatency() {
    return totalLatency / totalChunksPlayed;
  }
  
  public void addDelay(final int cycles) {
    TextLogger.log(client.network.getAddress(), "adding delay: " + delay + "\n");
    delay += cycles;
  }
  
  public Object toXml(final String prefix) {
    final StringBuilder sb = new StringBuilder();
    sb.append(prefix + "<playerModule ");
    if (waitIfNoChunk) {
      sb.append("waitIfNoChunk=\"true\" ");
    }
    if (bufferFromFirstChunk) {
      sb.append("bufferFromFirstChunk=\"true\" ");
    }
    sb.append("serverStartup=\"" + serverStartup + "\" ");
    sb.append("startupBuffering=\"" + startupBuffering + "\" ");
    sb.append("streamWindowSize=\"" + streamWindowSize + "\" />");
    return sb.toString();
  }
  
  public int getPlaySeconds() {
    return playSeconds;
  }
  
  public int getChunksPlayed() {
    return totalChunksPlayed;
  }
  
  public void newProtocol(final int currentConfNumber2) {
    currentConfNumber = currentConfNumber2;
  }
}
