package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

public class StreamGobbler extends Thread {
  InputStream is;
  OutputStream os;
  
  public StreamGobbler(final InputStream i, final OutputStream redirect) {
    is = i;
    os = redirect;
  }
  
  @Override public void run() {
    PrintWriter pw = null;
    try {
      if (os != null) {
        pw = new PrintWriter(os);
      }
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(isr);
      String line = null;
      while ((line = br.readLine()) != null) {
        if (pw != null) {
          pw.println(line);
          pw.flush();
        }
      }
    } catch (final IOException ioe) {
      ioe.printStackTrace();
    } finally {
      if (pw != null) {
        pw.close();
      }
    }
  }
}