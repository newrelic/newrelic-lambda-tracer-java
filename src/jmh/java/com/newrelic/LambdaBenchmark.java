/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic;

import com.newrelic.opentracing.LambdaTracer;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.State;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(value = org.openjdk.jmh.annotations.Scope.Benchmark)
public class LambdaBenchmark {

    static {
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    @Param({ "true", "false" })
    public boolean createErrors;

    @Param({ "true", "false" })
    public boolean createTags;

    @Param({ "1", "5", "10" })
    public int createSpans;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void createSpans() {
        final Span span = GlobalTracer.get().buildSpan("outer-span").start();
        try (Scope scope = GlobalTracer.get().activateSpan(span)) {
            createNestedSpan(span, createSpans);
        } finally {
            span.finish();
        }
    }

    private void createNestedSpan(Span span, int depth) {
        if (depth > 0) {
            final Span nestedSpan = GlobalTracer.get().buildSpan("nested-span-" + depth).start();
            try (Scope scope = GlobalTracer.get().activateSpan(nestedSpan)) {
                if (createTags) {
                    span.setTag("count", depth);
                }

                if (createErrors) {
                    try {
                        int x = 1 / 0;
                    } catch (Throwable throwable) {
                        final Map<String, Object> errorAttributes = new HashMap<>();
                        errorAttributes.put("event", Tags.ERROR.getKey());
                        errorAttributes.put("error.object", throwable);
                        errorAttributes.put("message", throwable.getMessage());
                        errorAttributes.put("stack", throwable.getStackTrace());
                        errorAttributes.put("error.kind", "Exception");
                        span.log(errorAttributes);
                    }
                }

                createNestedSpan(span, --depth);
            } finally {
                nestedSpan.finish();
            }
        }
    }

}
