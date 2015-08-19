package logging.logObjects;

import java.io.Serializable;

import utils.Common;

public abstract class DataLog implements Serializable {
  private static final long serialVersionUID = 455647864846L;
  public final Integer group;
  
  public DataLog(final String node) {
    this.node = node;
    if (node != null) {
      group = Common.currentConfiguration.getNodeGroup(node);
    } else {
      group = null;
    }
  }
  
  public final String node;
}
