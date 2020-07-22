/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.util;

import com.newrelic.opentracing.LambdaSpan;

import java.util.Map;

public class SpanCategoryDetection {

    private SpanCategoryDetection() {
    }

    public static SpanCategory detectSpanCategory(LambdaSpan span) {
        final Map<String, Object> tags = span.getTags();
        if (tags.containsKey("db.instance") || tags.containsKey("db.statement") || tags.containsKey("db.type") || tags.containsKey("db.user")) {
            return SpanCategory.DATASTORE;
        }
        if (tags.getOrDefault("span.kind", "").equals("client") &&
                (tags.containsKey("http.method") || tags.containsKey("http.status_code") || tags.containsKey("http.url"))) {
            return SpanCategory.HTTP;
        }
        return SpanCategory.GENERIC;
    }

}
