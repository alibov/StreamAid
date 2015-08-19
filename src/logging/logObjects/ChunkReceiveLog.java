package logging.logObjects;

import java.util.TreeSet;

import messages.ChunkMessage;
import utils.Utils;

public class ChunkReceiveLog extends DataLog {
  public final String sourceNode;
  public final long time;
  public final String destinationNode;
  public final long index;
  public final TreeSet<Integer> descriptors;

  public ChunkReceiveLog(final ChunkMessage message) {
    super(null);
    index = message.chunk.index;
    descriptors = message.chunk.getDescriptions();
    sourceNode = message.sourceId.toString();
    destinationNode = message.destID.toString();
    time = Utils.getMovieTime();
  }

  private static final long serialVersionUID = 2440139404233290306L;
}
