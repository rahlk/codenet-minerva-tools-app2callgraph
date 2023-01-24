/*
Copyright IBM Corporation 2022

Licensed under the Apache Public License 2.0, Version 2.0 (the "License");
you may not use this file except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.konveyor.dgi.code2graph.utils;

import java.time.LocalDateTime;

public class Log {
  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";
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
    toConsole(msg, ANSI_PURPLE, "INFO");
  }

  /** @param msg */
  public static final void done(String msg) {
    toConsole(msg, ANSI_GREEN, "DONE");
  }

  /** @param msg */
  public static final void debug(String msg) {
    toConsole(msg, ANSI_YELLOW, "DEBUG");
  }

  /** @param msg */
  public static final void error(String msg) {
    toConsole(msg, ANSI_RED, "ERROR");
  }

  /**
   * Print log message to console
   *
   * @param msg to print to console
   */
  private static void toConsole(String msg, String ansi_color, String Level) {
    if (isVerbose() == true) {
      LocalDateTime localDateTime = LocalDateTime.now();
      System.out.println(ANSI_CYAN + localDateTime.toString() + ANSI_RESET + ansi_color + "\t[" + Level + "]\t" + ANSI_RESET + msg);
    }
  }
}
