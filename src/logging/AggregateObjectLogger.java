package logging;

import logging.logObjects.DataLog;
import experiment.logAnalyzer.LogAnalyzer;

public class AggregateObjectLogger<T extends DataLog> extends ObjectLogger<T> {
  public AggregateObjectLogger(final String logName) {
    super(logName);
  }
  
  @Override protected void init() {
    // do nothing
  }
  
  @Override public void logObject(final T m) {
    LA.handleLogObject(m, logName);
    for (final LogAnalyzer vals : grouptoLA.values()) {
      vals.handleLogObject(m, logName);
    }
  }
  
  @Override protected void close() {
    // do nothing
  }
}
