package io.tackle.dgi.code2graph.utils;

import java.time.LocalDateTime;

public class Log {

    static private String LEVEL = "INFO";
    static private boolean verbose = true;

    /**
     * Set logging level.
     * 
     * @param level
     */
    static final public void setLogLevel(String level) {
        LEVEL = level;
    }

    /**
     * Set verbose setting to on or off.
     * 
     * @param val True or false.
     */
    static final public void setVerbosity(boolean val) {
        verbose = val;
    }

    /**
     * Is verbosity turned on/off 
     * 
     * @return Boolean 
     */
    static final public boolean isVerbose() {
        return verbose;
    }

    /**
     * Print log message to console
     * 
     * @param msg to print to console
     */
    static final public void toConsole(String msg) {
        if (isVerbose() == true) {
            LocalDateTime localDateTime = LocalDateTime.now();
            System.out.println(localDateTime.toString() + "\t[" + LEVEL + "]\t" + msg);
        }
    }
}
