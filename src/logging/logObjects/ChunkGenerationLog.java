package logging.logObjects;

public class ChunkGenerationLog extends DataLog {
  public final long index;
  public final long generationTime;
  
  public ChunkGenerationLog(final long index, final long generationTime) {
    super(null);
    this.index = index;
    this.generationTime = generationTime;
  }
  
  private static final long serialVersionUID = -5021972310498926438L;
}
