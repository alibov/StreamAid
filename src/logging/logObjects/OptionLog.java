package logging.logObjects;

import utils.Utils;

public class OptionLog extends DataLog {
  public final int chosenOption;
  public final long time;
  public final int currentPeriod;
  
  public OptionLog(final String node, final int currentPeriod, final int chosenOption) {
    super(node);
    this.currentPeriod = currentPeriod;
    time = Utils.getMovieTime();
    this.chosenOption = chosenOption;
  }
  
  private static final long serialVersionUID = -7567907520085503177L;
}
