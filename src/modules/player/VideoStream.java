package modules.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import logging.logObjects.ChunkPlayLog;
import utils.Common;
import utils.Utils;
import entites.Bitmap;
import entites.MDCVideoStreamChunk;
import entites.PrimeNodeReport;
import entites.VideoStreamChunk;

public class VideoStream {
  static public int safetyBuffer = 120;
  public long windowOffset;
  public double playSpeed = 1.0;
  final static public double qualityThreshold = ((double) (Common.currentConfiguration.descriptions - 1))
      / Common.currentConfiguration.descriptions; // 1.0 /
  // Common.currentConfiguration.descriptions;
  public static final long startingFrame = 1;
  public double threshold = qualityThreshold;
  ArrayList<VideoStreamChunk> victimCache = new ArrayList<VideoStreamChunk>();
  ArrayList<VideoStreamChunk> chunkArray = new ArrayList<VideoStreamChunk>();
  private final List<StreamListener> listeners = new LinkedList<StreamListener>();
  private final long playbackStartTime;
  private double positionInChunk = 0.0;
  LinkedList<ChunkPlayLog> chunkPlayed = new LinkedList<ChunkPlayLog>();
  private SkipPolicy skipPolicy = new SkipPolicy() {
    @Override public boolean skipChunk(final VideoStream vs) {
      return !waitIfNoChunk;
    }
  };
  final boolean waitIfNoChunk;
  private final int startupBuffering;
  
  public VideoStream(final long offset, final int streamWindowSize, final boolean waitIfNoChunk, final int startupBuffering) {
    windowOffset = offset;
    this.startupBuffering = startupBuffering;
    this.waitIfNoChunk = waitIfNoChunk;
    for (int i = 0; i < streamWindowSize; i++) {
      chunkArray.add(null);
    }
    playbackStartTime = Utils.getMovieTime();
  }
  
  public boolean updateChunks(final Set<VideoStreamChunk> chunks) {
    final Set<Long> updatedChunks = new TreeSet<Long>();
    for (final VideoStreamChunk chunk : chunks) {
      if (chunk instanceof MDCVideoStreamChunk) {
        if (chunk.index >= windowOffset && chunk.index - windowOffset < chunkArray.size()) {
          MDCVideoStreamChunk MDCvsc = (MDCVideoStreamChunk) chunkArray.get((int) (chunk.index - windowOffset));
          if (MDCvsc == null || MDCvsc.index != chunk.index) {
            MDCvsc = (MDCVideoStreamChunk) chunk;
          } else {
            MDCvsc.combineChunk((MDCVideoStreamChunk) chunk);
          }
          chunkArray.set((int) (chunk.index - windowOffset), MDCvsc);
          updatedChunks.add(chunk.index);
        } else if (chunk.index < windowOffset && windowOffset - chunk.index < victimCache.size()) {
          MDCVideoStreamChunk MDCvsc = (MDCVideoStreamChunk) victimCache
              .get((int) (victimCache.size() - (windowOffset - chunk.index)));
          if (MDCvsc == null || MDCvsc.index != chunk.index) {
            MDCvsc = (MDCVideoStreamChunk) chunk;
          } else {
            MDCvsc.combineChunk((MDCVideoStreamChunk) chunk);
          }
          victimCache.set((int) (victimCache.size() - (windowOffset - chunk.index)), MDCvsc);
          updatedChunks.add(chunk.index);
        }
      } else {
        if (chunk.index >= windowOffset && chunk.index - windowOffset < chunkArray.size()) {
          chunkArray.set((int) (chunk.index - windowOffset), chunk);
          updatedChunks.add(chunk.index);
        } else if (chunk.index < windowOffset && windowOffset - chunk.index < victimCache.size()) {
          victimCache.set((int) (victimCache.size() - (windowOffset - chunk.index)), chunk);
          updatedChunks.add(chunk.index);
        }
      }
    }
    if (updatedChunks.isEmpty()) {
      return false;
    }
    for (final StreamListener l : listeners) {
      l.onStreamUpdate(updatedChunks);
    }
    return true;
  }
  
  // returns play amount (in movie seconds)
  public double playChunk() {
    if (Utils.getMovieTime() < playbackStartTime) {
      throw new RuntimeException("playChunk called before " + playbackStartTime);
    }
    double played = 0.0;
    double prevPosition = positionInChunk;
    positionInChunk += playSpeed;
    if (positionInChunk < 1.0 && prevPosition > 0.0) {
      return playSpeed;
    }
    while (positionInChunk >= 1.0) {
      positionInChunk -= 1.0;
      if (!canPlay()) {
        if (!skipPolicy.skipChunk(this)) {
          positionInChunk = 0.0;
          return played;
        }
      } else {
        if (prevPosition == 0.0) {
          final long delta = (long) (played / playSpeed * Common.currentConfiguration.cycleLength);
          chunkPlayed.add(new ChunkPlayLog(chunkArray.get(0).index, null, Utils.getMovieTime() + delta, chunkArray.get(0)
              .getQuality()));
        }
        played += 1.0 - prevPosition;
        prevPosition = 0.0;
      }
      advanceWindow();
    }
    if (!canPlay()) {
      if (!skipPolicy.skipChunk(this)) {
        positionInChunk = 0.0;
      }
    } else if (prevPosition == 0.0 && positionInChunk > 0.0) {
      final long delta = (long) ((playSpeed - positionInChunk) / playSpeed * Common.currentConfiguration.cycleLength);
      chunkPlayed
          .add(new ChunkPlayLog(chunkArray.get(0).index, null, Utils.getMovieTime() + delta, chunkArray.get(0).getQuality()));
      played += positionInChunk;
    }
    return played;
  }
  
  protected void advanceWindow() {
    victimCache.add(chunkArray.remove(0));
    chunkArray.add(null);
    windowOffset++;
    if (victimCache.size() > safetyBuffer) {
      victimCache.remove(0);
    }
  }
  
  public Bitmap getBitmap(final long startingFrameAskedFor) {
    final Set<Integer> bitmap = new TreeSet<Integer>();
    for (int i = 0; i < chunkArray.size(); ++i) {
      if (chunkArray.get(i) != null && i + windowOffset >= startingFrameAskedFor) {
        bitmap.add((int) (i + windowOffset - startingFrameAskedFor));
      }
    }
    // already played chunks
    for (int i = (int) (windowOffset - startingFrameAskedFor - 1), j = victimCache.size() - 1; i >= 0 && j >= 0; j--, i--) {
      if (victimCache.get(j) != null) { // can be null if skipping is allowed
        bitmap.add(i);
      }
    }
    return new Bitmap(bitmap, startingFrameAskedFor);
  }
  
  public PrimeNodeReport getAvailableDescriptions(final long startingFrameAskedFor) {
    final PrimeNodeReport retVal = new PrimeNodeReport();
    for (int i = 0; i < chunkArray.size(); ++i) {
      if (chunkArray.get(i) != null && i + windowOffset >= startingFrameAskedFor) {
        retVal.put(i + windowOffset, chunkArray.get(i).getDescriptions());
      }
    }
    // already played chunks
    for (int i = (int) (windowOffset - 1), j = (victimCache.size() - 1); i >= 0 && j >= 0; j--, i--) {
      if (victimCache.get(j) != null) { // can be null if skipping is allowed
        retVal.put((long) i, victimCache.get(j).getDescriptions());
      }
    }
    return retVal;
  }
  
  public PrimeNodeReport getMissingDescriptions() {
    // TODO: handle threshold in the calling function.
    final PrimeNodeReport retVal = new PrimeNodeReport();
    for (int i = 0; i < chunkArray.size(); ++i) {
      if (chunkArray.get(i) == null) {
        retVal.put(i + windowOffset, PrimeNodeReport.getDifferenceSet(new TreeSet<Integer>()));
      } else {
        final TreeSet<Integer> descriptions = PrimeNodeReport.getDifferenceSet(chunkArray.get(i).getDescriptions());
        final boolean hasAllDescriptions = (descriptions.size() == 0) ? true : false;
        if (!hasAllDescriptions) {
          retVal.put(i + windowOffset, descriptions);
        }
      }
    }
    return retVal;
  }
  
  public VideoStreamChunk getChunk(final Long reqChunk) {
    if (reqChunk - windowOffset < -victimCache.size()) {
      // throw new RuntimeException("not enough victims in victim cache!");
      return null;
    }
    try {
      if (reqChunk - windowOffset < 0) {
        return victimCache.get((int) (reqChunk - windowOffset + victimCache.size()));
      }
      if (reqChunk - windowOffset >= safetyBuffer) {
        System.err.println("requesting nonexistant chunk: " + reqChunk);
        return null;
      }
      return chunkArray.get((int) (reqChunk - windowOffset));
    } catch (final Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public void nextCycle() {
    // do nothing here
  }
  
  public long getEarliestChunk() {
    for (int i = 0; i < victimCache.size(); ++i) {
      if (victimCache.get(i) != null) {
        return victimCache.get(i).index;
      }
    }
    for (int i = 0; i < chunkArray.size(); ++i) {
      if (chunkArray.get(i) != null) {
        return chunkArray.get(i).index;
      }
    }
    return -1;
  }
  
  public VideoStreamChunk getLatestChunk() {
    VideoStreamChunk retVal = null;
    for (final VideoStreamChunk ch : chunkArray) {
      if (ch != null) {
        retVal = ch;
      }
    }
    if (retVal == null) {
      if (!victimCache.isEmpty()) {
        retVal = victimCache.get(victimCache.size() - 1);
      } else {
        return null;
      }
    }
    return retVal;
  }
  
  public long getFirstMissingChunk() {
    int i = 0;
    for (final VideoStreamChunk ch : chunkArray) {
      if (ch == null) {
        return windowOffset + i;
      }
      i++;
    }
    return windowOffset + chunkArray.size();
  }
  
  public void addListener(final StreamListener listener) {
    listeners.add(listener);
  }
  
  public void addListeners(final Collection<StreamListener> listeners2) {
    listeners.addAll(listeners2);
  }
  
  public void removeListener(final StreamListener listener) {
    listeners.remove(listener);
  }
  
  public boolean updateChunk(final VideoStreamChunk chunk) {
    return updateChunks(Collections.singleton(chunk));
  }
  
  public Long getAvailableTime(final long i) {
    return (i - windowOffset + 1) * 1000;
  }
  
  public boolean isBuffering() {
    return Utils.getMovieTime() < playbackStartTime + startupBuffering;
  }
  
  public Set<Long> getMissingChunks(final long minChunk, final long maxChunk) {
    final Set<Long> retVal = new TreeSet<Long>();
    for (int i = Math.max(skipPolicy.skipChunk(this) && !isBuffering() ? 1 : 0, (int) (minChunk - windowOffset)); i < chunkArray
        .size() && (windowOffset + i) <= maxChunk; ++i) {
      // Movie time might be out of date!
      /* if (Utils.getMovieTime() < (windowOffset + i) * 1000) { break; } */
      if (chunkArray.get(i) == null) {
        retVal.add(i + windowOffset);
      }
    }
    return retVal;
  }
  
  public void setSkipPolicy(final SkipPolicy policy) {
    skipPolicy = policy;
  }
  
  public double getLag() {
    return (Utils.getMovieTime() - windowOffset * Common.currentConfiguration.cycleLength - positionInChunk)
        / Common.currentConfiguration.cycleLength;
  }
  
  public boolean canPlay() {
    return !(chunkArray.get(0) == null || chunkArray.get(0).getQuality() < threshold);
  }
}
