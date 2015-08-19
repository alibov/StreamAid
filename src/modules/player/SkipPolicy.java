package modules.player;


public interface SkipPolicy {
  boolean skipChunk(VideoStream vs);
}
