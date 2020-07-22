package com.newrelic.opentracing.logging;

import java.util.LinkedList;
import java.util.List;

/**
 * The in-memory logger does not write to standard out, but instead saves all messages
 * in memory, and can be used by tests to check if certain messages were logged.
 */
public class InMemoryLogger implements Logger {

    private final List<String> logs = new LinkedList<>();

    @Override
    public void out(String message) {
        logs.add(message);
    }

    @Override
    public void debug(String message) {
        logs.add(message);
    }

    @Override
    public List<String> getLogs() {
        return logs;
    }

}
