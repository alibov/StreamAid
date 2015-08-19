package logging.logObjects;

public class ChunkPlayLog extends DataLog {
  private static final long serialVersionUID = 8888313506912563606L;
  public final long chunkIndex;
  public final double time;
  public final double quality;
  
  public ChunkPlayLog(final long chunkIndex, final String node, final double time, final double quality) {
    super(node);
    this.chunkIndex = chunkIndex;
    this.time = time;
    this.quality = quality;
  }
}
