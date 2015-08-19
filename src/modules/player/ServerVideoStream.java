package modules.player;

import java.util.Set;

import logging.ObjectLogger;
import logging.logObjects.ChunkGenerationLog;
import utils.Utils;
import entites.VideoStreamChunk;

public class ServerVideoStream extends VideoStream {
  private static ObjectLogger<ChunkGenerationLog> chunkGenLogger = ObjectLogger.getLogger("chunkGen");
  private boolean startPlayback = false;
  private final int serverStartup;
  
  public ServerVideoStream(final int serverStartup, final int streamWindowSize, final boolean waitIfNoChunk,
      final int startupBuffering) {
    super(VideoStream.startingFrame, streamWindowSize, waitIfNoChunk, startupBuffering);
    this.serverStartup = serverStartup;
  }
  
  private static VideoStreamChunk newChunkContent(final long index) {
    chunkGenLogger.logObject(new ChunkGenerationLog(index, Utils.getMovieTime()));
    return new VideoStreamChunk(index);
  }
  
  @Override public Set<Long> getMissingChunks(final long min, final long max) {
    throw new RuntimeException("getMissingChunks called on server node");
  }
  
  @Override public boolean updateChunks(final Set<VideoStreamChunk> chunks) {
    throw new RuntimeException("shouldn't get here");
  }
  
  @Override public void nextCycle() {
    if (!startPlayback || Utils.getMovieTime() < 0) {
      return;
    }
    assert (chunkArray.get(0) == null);
    chunkArray.set(0, newChunkContent(windowOffset));
  }
  
  @Override protected void advanceWindow() {
    victimCache.add(chunkArray.remove(0));
    chunkArray.add(null);
    windowOffset++;
  }
  
  public void play() {
    startPlayback = true;
    Utils.movieStartTime = Utils.getTime() + serverStartup;
  }
  
  @Override public boolean isBuffering() {
    return false;
  }
}
