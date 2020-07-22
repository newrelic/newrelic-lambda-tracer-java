package com.newrelic.opentracing.logging;

import java.util.LinkedList;
import java.util.List;

/**
 * The console logger should only be used for writing payload data to standard out.
 * It should be the default logger and it should ignore debug messages.
 */
public class ConsoleLogger implements Logger {

    @Override
    public void out(String message) {
        System.out.println(message);
    }

    @Override
    public void debug(String message) {
    }

    @Override
    public List<String> getLogs() {
        return new LinkedList<>();
    }

}
