package com.newrelic.opentracing.logging;

import java.util.List;

public interface Logger {

    /**
     * Writes to standard out. Should only be used to write payload data.
     *
     * @param message String to be logged
     */
    void out(String message);

    /**
     * Writes to standard out. Can be used to also write debug messages for trouble-shooting.
     *
     * @param message String to be logged
     */
    void debug(String message);

    /**
     * Return a list of all logged messages. In most implementations this will be a no-op.
     *
     * @return List of all logged message Strings
     */
    List<String> getLogs();

}
