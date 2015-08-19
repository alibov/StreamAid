package messages;

import entites.Bitmap;
import experiment.frameworks.NodeAddress;

/**
 * if startingFrame is -1 - it is a bitmap reply
 * 
 * @author alibov
 * 
 */
public class BitmapUpdateMessage extends Message {
  public final Bitmap bitmap;
  public final long earliestChunkAvailable;
  private static final long serialVersionUID = 6738237054928020871L;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Long.SIZE + bitmap.getSimulatedSize();
  }
  
  public BitmapUpdateMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Bitmap bitmap, final long earliestChunkAvailable) {
    super(tag, sourceId, destID);
    this.bitmap = bitmap;
    this.earliestChunkAvailable = earliestChunkAvailable;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  public boolean isEmpty() {
    if (earliestChunkAvailable != bitmap.startingFrame) {
      return false;
    }
    return bitmap.isEmpty();
  }
  
  @Override protected String getContents() {
    return "bm: " + bitmap + "earliest: " + earliestChunkAvailable;
  }
}
