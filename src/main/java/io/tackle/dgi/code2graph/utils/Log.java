package io.tackle.dgi.code2graph.utils;

import java.time.LocalDateTime;

public class Log {

  private static boolean verbose = true;

  /**
   * Set verbose setting to on or off.
   *
   * @param val True or false.
   */
  public static final void setVerbosity(boolean val) {
    verbose = val;
  }

  /**
   * Is verbosity turned on/off
   *
   * @return Boolean
   */
  public static final boolean isVerbose() {
    return verbose;
  }

  /** @param msg */
  public static final void info(String msg) {
    toConsole(msg, "INFO");
  }

  /** @param msg */
  public static final void done(String msg) {
    toConsole(msg, "DONE");
  }

  /** @param msg */
  public static final void dedug(String msg) {
    toConsole(msg, "DEBUG");
  }

  /** @param msg */
  public static final void error(String msg) {
    toConsole(msg, "ERROR");
  }

  /**
   * Print log message to console
   *
   * @param msg to print to console
   */
  private static void toConsole(String msg, String Level) {
    if (isVerbose() == true) {
      LocalDateTime localDateTime = LocalDateTime.now();
      System.out.println(localDateTime.toString() + "\t[" + Level + "]\t" + msg);
    }
  }
}
