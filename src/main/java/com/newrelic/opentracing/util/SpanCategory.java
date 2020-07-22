/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
