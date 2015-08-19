package experiment.logAnalyzer;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

public class NodeInfo implements Serializable {
  private static final long serialVersionUID = -8642274556052843820L;
  public int alg;
  public long startupBuffer;
  public long cycle;
  public long joinTime;
  public long leaveTime;
  public Map<Long/* chunk */, NodeChunkInfo> chunkInfo = new TreeMap<Long, NodeChunkInfo>();
  public long lastChunkPlayed;
  public double chunkOrderAvg;
  public double chunkLatencyAvg;
  public long messagesSent;
  public long bytesSent;
  public long overheadMessagesSent;
  public long overheadBytesSent;
  public long duplicateChunkMessagesSent;
  public long duplicateChunkBytesSent;
  public double startupDelay;
  public double lag;
  public double lastDelay;
  public boolean bufferFromFirstChunk;
  public long playSeconds = 0L;
  public double ci = 0.0;
  public long totalUsedUploadBandwidth = 0L;
}