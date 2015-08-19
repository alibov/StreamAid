package modules.player;

import java.util.Set;

public interface StreamListener {
  public void onStreamUpdate(Set<Long> updatedChunks);
}
