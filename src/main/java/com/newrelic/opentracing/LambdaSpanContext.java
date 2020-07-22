/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
import io.opentracing.SpanContext;

import java.util.Map;

public class LambdaSpanContext implements SpanContext {

    private final LambdaSpan span;
    private final LambdaScopeManager scopeManager;

    LambdaSpanContext(LambdaSpan span, LambdaScopeManager scopeManager) {
        this.span = span;
        this.scopeManager = scopeManager;
    }

    public DistributedTracingState getDistributedTracingState() {
        return scopeManager.dtState.get();
    }

    PrioritySamplingState getPrioritySamplingState() {
        return scopeManager.priorityState.get();
    }

    public TransactionState getTransactionState() {
        return scopeManager.txnState.get();
    }

    LambdaScopeManager getScopeManager() {
        return scopeManager;
    }

    LambdaSpan getSpan() {
        return span;
    }

    @Override
    public String toTraceId() {
        return span.traceId();
    }

    @Override
    public String toSpanId() {
        return span.guid();
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return getDistributedTracingState().getBaggage().entrySet();
    }

    void spanFinished(LambdaSpan lambdaSpan) {
        scopeManager.dataCollection.get().spanFinished(lambdaSpan);
    }

}
