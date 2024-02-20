/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
import com.newrelic.opentracing.util.DistributedTraceUtil;
import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tag;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LambdaSpanBuilder implements Tracer.SpanBuilder {

    private long startTimeInNanos;
    private boolean ignoreActiveSpan = false;
    private SpanContext parent;

    private final String operationName;
    private final Map<String, Object> tags = new HashMap<>();

    LambdaSpanBuilder(String operationName) {
        this.operationName = operationName;
    }

    @Override
    public Tracer.SpanBuilder asChildOf(SpanContext parent) {
        return addReference(References.CHILD_OF, parent);
    }

    @Override
    public Tracer.SpanBuilder asChildOf(Span parent) {
        return asChildOf(parent.context());
    }

    @Override
    public Tracer.SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
        if (parent == null && (referenceType.equals(References.CHILD_OF) || referenceType.equals(References.FOLLOWS_FROM))) {
            this.parent = referencedContext;
        }
        return this;
    }

    @Override
    public Tracer.SpanBuilder ignoreActiveSpan() {
        this.ignoreActiveSpan = true;
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, String value) {
        if (key != null && value != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, boolean value) {
        if (key != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, Number value) {
        if (key != null && value != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public <T> Tracer.SpanBuilder withTag(Tag<T> tag, T value) {
        if (tag != null) {
            if (tag.getKey() != null && value != null) {
                tags.put(tag.getKey(), value);
            }
        }
        return this;
    }

    @Override
    public Tracer.SpanBuilder withStartTimestamp(long microseconds) {
        startTimeInNanos = TimeUnit.MICROSECONDS.toNanos(microseconds);
        return this;
    }

    @Override
    public Span start() {
        long timestamp = System.currentTimeMillis();
        long startTimeInNanos = this.startTimeInNanos == 0 ? System.nanoTime() : this.startTimeInNanos;

        final LambdaTracer tracer = LambdaTracer.INSTANCE;
        final Span activeSpan = tracer.activeSpan();

        // Figure out the parent context situation
        SpanContext parentSpanContext = null;
        if (!ignoreActiveSpan && parent == null && activeSpan != null) {
            // An explicit parent hasn't been set, and there's an active span we're not ignoring.
            addReference(References.CHILD_OF, activeSpan.context());
            parentSpanContext = activeSpan.context();
        } else if (parent != null) {
            // We have an explicit parent context
            parentSpanContext = parent;
        }

        // Construct the new span, wiring in the parent context
        LambdaSpan newSpan;
        if (parentSpanContext instanceof LambdaPayloadContext) {
            // Our parent context is extracted from a cross-process trace context. New root span.
            final LambdaPayloadContext payloadContext = (LambdaPayloadContext) parentSpanContext;
            final DistributedTracePayloadImpl dtPayload = payloadContext.getPayload();
            final PrioritySamplingState prioritySamplingState = PrioritySamplingState.create(dtPayload);

            final DistributedTracingState distributedTracingState = new DistributedTracingState(payloadContext);
            TransactionState transactionState = new TransactionState();

            newSpan = new LambdaSpan(operationName, timestamp, startTimeInNanos, tags, null, DistributedTraceUtil.generateGuid());
            LambdaSpanContext spanContext = new LambdaSpanContext(newSpan, distributedTracingState, prioritySamplingState, transactionState, new LambdaCollector());
            newSpan.setContext(spanContext);
        } else if (parentSpanContext instanceof LambdaSpanContext) {
            // Our parent context is a normal, local span context
            final LambdaSpanContext lambdaSpanContext = (LambdaSpanContext) parentSpanContext;
            LambdaSpan parentSpan = lambdaSpanContext.getSpan();
            newSpan = new LambdaSpan(operationName, timestamp, startTimeInNanos, tags, parentSpan, DistributedTraceUtil.generateGuid());
            newSpan.setContext(lambdaSpanContext.newContext(newSpan));
        } else {
            // We have no parent context. New root span, new trace.
            newSpan = new LambdaSpan(operationName, timestamp, startTimeInNanos, tags, null, DistributedTraceUtil.generateGuid());
            final AdaptiveSampling adaptiveSampling = tracer.adaptiveSampling();
            adaptiveSampling.requestStarted();
            final PrioritySamplingState pss = PrioritySamplingState.setSampledAndGeneratePriority(adaptiveSampling.computeSampled());
            newSpan.setContext(new LambdaSpanContext(newSpan, new DistributedTracingState(), pss, new TransactionState(), new LambdaCollector()));
        }

        return newSpan;
    }

}
