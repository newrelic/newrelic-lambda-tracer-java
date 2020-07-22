package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;

import java.util.HashMap;
import java.util.Map;

public class SpanTestUtils {
    public static LambdaSpan createSpan(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, LambdaSpan parentSpan, String guid,
                                        String txnId) {
        final LambdaSpan span = new LambdaSpan(operationName, timestamp, nanoTime, tags, parentSpan, guid, txnId);
        final LambdaSpanContext context = new LambdaSpanContext(span, new LambdaScopeManager());
        span.setContext(context);
        return span;
    }

    public static LambdaSpan createSpanWithInboundPayload(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, String guid) {
        // Span to create the inbound payload from
        final LambdaSpan inboundSpan = new LambdaSpan("GET", timestamp, nanoTime, new HashMap<>(), null, "guidInbound", "txnIdInbound");

        // DT state
        final DistributedTracingState distributedTracingState = new DistributedTracingState();
        distributedTracingState.generateAndStoreTraceId();

        final TransactionState transactionState = new TransactionState();
        transactionState.setTransactionDuration(33.4f);
        transactionState.setTransactionName("Other", "Test");

        final DataCollection dataCollection = new DataCollection();

        final PrioritySamplingState prioritySamplingState = new PrioritySamplingState();
        prioritySamplingState.setSampled(true);
        prioritySamplingState.setPriority(1.2345f);

        // Set state on lambda scope manager
        final LambdaScopeManager lambdaScopeManager = new LambdaScopeManager();
        lambdaScopeManager.dtState.set(distributedTracingState);
        lambdaScopeManager.txnState.set(transactionState);
        lambdaScopeManager.dataCollection.set(dataCollection);
        lambdaScopeManager.priorityState.set(prioritySamplingState);

        // Set context for inbound span
        final LambdaSpanContext inboundSpanContext = new LambdaSpanContext(inboundSpan, lambdaScopeManager);
        inboundSpan.setContext(inboundSpanContext);

        // Create DT payload from inboundSpan
        final DistributedTracePayloadImpl distributedTracePayload = DistributedTracing.getInstance().createDistributedTracePayload(inboundSpan);

        // Add DT payload to DT state
        distributedTracingState.setInboundPayloadAndTransportTime(distributedTracePayload, 1337);

        // Create current span and context
        final LambdaSpan currentSpan = new LambdaSpan(operationName, timestamp + 1, nanoTime + 1, tags, inboundSpan, guid, distributedTracingState.getInboundPayload().getTransactionId());
        final LambdaSpanContext currentSpanContext = new LambdaSpanContext(currentSpan, lambdaScopeManager);
        currentSpan.setContext(currentSpanContext);

        return currentSpan;
    }
}
