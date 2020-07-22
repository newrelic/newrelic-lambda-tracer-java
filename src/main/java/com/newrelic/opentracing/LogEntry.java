/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
