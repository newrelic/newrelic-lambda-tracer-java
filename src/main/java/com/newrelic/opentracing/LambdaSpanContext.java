/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayload;
import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
import io.opentracing.SpanContext;

import java.util.Map;

public class LambdaSpanContext implements SpanContext {

    private final LambdaSpan span;
    private final DistributedTracingState distributedTracingState;
    private final PrioritySamplingState prioritySamplingState;
    private final TransactionState transactionState;
    private final LambdaCollector lambdaCollector;

    LambdaSpanContext(LambdaSpan span,
                      DistributedTracingState distributedTracingState,
                      PrioritySamplingState prioritySamplingState,
                      TransactionState transactionState,
                      LambdaCollector lambdaCollector) {
        this.span = span;
        this.distributedTracingState = distributedTracingState;
        this.prioritySamplingState = prioritySamplingState;
        this.transactionState = transactionState;
        this.lambdaCollector = lambdaCollector;
    }

    @Override
    public String toTraceId() {
        return distributedTracingState.getTraceId();
    }

    @Override
    public String toSpanId() {
        return span.guid();
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return distributedTracingState.getBaggage().entrySet();
    }

    public LambdaSpan getSpan() {
        return span;
    }

    public String getParentId() {
        if (distributedTracingState != null) {
            final DistributedTracePayloadImpl inboundPayload = distributedTracingState.getInboundPayload();
            if (inboundPayload != null && inboundPayload.hasGuid()) {
                return inboundPayload.getGuid();
            }
        }
        return null;
    }

    public float getPriority() {
        return prioritySamplingState.getPriority();
    }

    public boolean isSampled() {
        return prioritySamplingState.isSampled();
    }

    public String getTransactionId() {
        return transactionState.getTransactionId();
    }

    public Map<String, Object> getDistributedTracingAttributes() {
        final DistributedTracing dt = DistributedTracing.getInstance();
        return dt.getDistributedTracingAttributes(distributedTracingState, span.guid(), getPriority());
    }

    public void setError() {
        transactionState.setError();
    }

    public void setTransactionDuration(float durationInSeconds) {
        transactionState.setTransactionDuration(durationInSeconds);
    }

    public void setTransactionName(String transactionType, String name) {
        transactionState.setTransactionName(transactionType, name);
    }

    public LambdaSpanContext newContext(LambdaSpan lambdaSpan) {
        return new LambdaSpanContext(lambdaSpan, distributedTracingState, prioritySamplingState, transactionState, lambdaCollector);
    }

    public DistributedTracePayload createDistributedTracingPayload() {
        return distributedTracingState.createDistributedTracingPayload(span);
    }

    public void collect() {
        lambdaCollector.spanFinished(this, distributedTracingState, transactionState);
    }
}
