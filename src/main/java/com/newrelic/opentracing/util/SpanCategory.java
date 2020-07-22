package com.newrelic.opentracing.util;

public enum SpanCategory {
    HTTP,
    DATASTORE,
    GENERIC;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
