package logging;

import java.util.LinkedList;
import java.util.List;

import logging.logObjects.DataLog;

public class InMemoryObjectLogger<T extends DataLog> extends ObjectLogger<T> {
  public List<T> objList = new LinkedList<T>();
  
  public InMemoryObjectLogger(final String logName) {
    super(logName);
  }
  
  @Override public synchronized void logObject(final T m) {
    objList.add(m);
    return;
  }
  
  @Override protected void close() {
    objLists.put(logName, objList);
  }
  
  @Override protected void init() {
    objList.clear();
  }
}
