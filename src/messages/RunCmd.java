package messages;

import java.util.Arrays;

import experiment.frameworks.NodeAddress;

public class RunCmd extends Message {
  private static final long serialVersionUID = 1879654994932678947L;
  public final String[] cmd;
  
  public RunCmd(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final String[] cmd) {
    super(tag, sourceId, destID);
    this.cmd = cmd;
  }
  
  @Override protected String getContents() {
    return Arrays.toString(cmd);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
