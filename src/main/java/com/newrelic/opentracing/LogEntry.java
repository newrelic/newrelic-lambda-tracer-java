package com.newrelic.opentracing;

public class LogEntry {

    private final long timestamp;
    private final Object value;

    public LogEntry(long timestampInMillis, Object value) {
        this.timestamp = timestampInMillis;
        this.value = value;
    }

    public long getTimestampInMillis() {
        return timestamp;
    }

    public Object getValue() {
        return value;
    }

}
