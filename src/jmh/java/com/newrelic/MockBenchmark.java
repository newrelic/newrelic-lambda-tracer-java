package com.newrelic;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@State(value = org.openjdk.jmh.annotations.Scope.Benchmark)
public class MockBenchmark {

    static {
        GlobalTracer.registerIfAbsent(new MockTracer());
    }

    @Param({ "true", "false" })
    public boolean createTags;

    @Param({ "1", "5", "10" })
    public int createSpans;

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void createMockSpans() {
        final Span span = GlobalTracer.get().buildSpan("outer-span").start();
        try (Scope scope = GlobalTracer.get().activateSpan(span)) {
            createNestedMockSpan(span, createSpans);
        } finally {
            span.finish();
        }
    }

    private void createNestedMockSpan(Span span, int depth) {
        if (depth > 0) {
            final Span nestedSpan = GlobalTracer.get().buildSpan("nested-span-" + depth).start();
            try (Scope scope = GlobalTracer.get().activateSpan(nestedSpan)) {
                if (createTags) {
                    span.setTag("count", depth);
                }

                createNestedMockSpan(span, --depth);
            } finally {
                nestedSpan.finish();
            }
        }
    }

}
