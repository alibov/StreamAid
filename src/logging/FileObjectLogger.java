package logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import logging.logObjects.DataLog;

public class FileObjectLogger<T extends DataLog> extends ObjectLogger<T> {
  private static int flushTimeout = 50;
  private static int currFlush = flushTimeout;
  private final String fileName;
  ObjectOutputStream out;
  
  public FileObjectLogger(final String logName) {
    super(logName);
    this.fileName = dirName + File.separator + logName;
    FileOutputStream fstream;
    try {
      fstream = new FileOutputStream(fileName);
      out = new ObjectOutputStream(fstream);
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  @Override public synchronized void logObject(final T m) {
    try {
      out.writeObject(m);
      currFlush--;
      if (currFlush == 0) {
        out.flush();
        currFlush = flushTimeout;
      }
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  @Override protected void close() {
    try {
      out.flush();
      out.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }
  
  @Override protected void init() {
    // do nothing
  }
}
