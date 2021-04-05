/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SpanTestUtils {
    public static LambdaSpan createSpan(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, LambdaSpan parentSpan, String guid) {
        return createSpan(operationName, timestamp, nanoTime, tags, parentSpan, guid, new PrioritySamplingState(1.1f, true));
    }

    public static LambdaSpan createSpan(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, LambdaSpan parentSpan, String guid, PrioritySamplingState prioritySamplingState) {
        final LambdaSpan span = new LambdaSpan(operationName, timestamp, nanoTime, tags, parentSpan, guid);

        final LambdaSpanContext context;
        if (parentSpan == null) {
            context = new LambdaSpanContext(span,
                    new DistributedTracingState(),
                    prioritySamplingState,
                    new TransactionState(),
                    new LambdaCollector());
        } else {
            context = ((LambdaSpanContext) parentSpan.context()).newContext(span);
        }
        span.setContext(context);
        return span;
    }

    public static LambdaSpan createSpanWithInboundPayload(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, String guid, LambdaCollector collector) {
        // Span to create the inbound payload from
        final LambdaSpan inboundSpan = new LambdaSpan("GET", timestamp, nanoTime, new HashMap<>(), null, "guidInbound");

        // DT state
        final DistributedTracingState distributedTracingState = new DistributedTracingState();

        final TransactionState transactionState = new TransactionState();
        transactionState.setTransactionDuration(33.4f);
        transactionState.setTransactionName("Other", "Test");

        final PrioritySamplingState prioritySamplingState = new PrioritySamplingState(1.2345f, true);

        // Set context for inbound span
        final LambdaSpanContext inboundSpanContext = new LambdaSpanContext(inboundSpan, distributedTracingState, prioritySamplingState, transactionState, new LambdaCollector());
        inboundSpan.setContext(inboundSpanContext);

        // Create DT payload from inboundSpan
        final DistributedTracePayloadImpl distributedTracePayload = DistributedTracing.getInstance().createDistributedTracePayload(inboundSpan);

        // Add DT payload to DT state
        LambdaPayloadContext currentContext = new LambdaPayloadContext(distributedTracePayload, 1337, Collections.emptyMap());

        // Create current span and context
        final LambdaSpan currentSpan = new LambdaSpan(operationName, timestamp + 1, nanoTime + 1, tags, null, guid);
        final LambdaSpanContext currentSpanContext = new LambdaSpanContext(currentSpan, new DistributedTracingState(currentContext), prioritySamplingState, transactionState, collector);
        currentSpan.setContext(currentSpanContext);

        return currentSpan;
    }
}
