package com.newrelic.opentracing.logging;

import java.util.LinkedList;
import java.util.List;

/**
 * The debug logger writes payload data to standard out, like the console logger, but also logs
 * debug messages. It should only be used if a certain environment variable is present.
 */
public class DebugLogger implements Logger {

    @Override
    public void out(String message) {
        System.out.println(message);
    }

    @Override
    public void debug(String message) {
        System.out.println("nr_debug: " + message);
    }

    @Override
    public List<String> getLogs() {
        return new LinkedList<>();
    }

}
