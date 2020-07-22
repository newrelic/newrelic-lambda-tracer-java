package com.newrelic.opentracing.events;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.LambdaSpanContext;
import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.state.DistributedTracingState;

import java.util.HashMap;
import java.util.Map;

public class TransactionEvent extends Event {

    private final Map<String, Object> intrinsics = new HashMap<>();
    private final Map<String, Object> userAttributes = new HashMap<>();
    private final Map<String, Object> agentAttributes = new HashMap<>();

    public TransactionEvent(LambdaSpan span) {
        intrinsics.put("type", "Transaction");
        intrinsics.put("timestamp", span.getTimestamp());
        intrinsics.put("duration", span.getDurationInSeconds());

        if (span.context() != null && span.context() instanceof LambdaSpanContext) {
            LambdaSpanContext context = (LambdaSpanContext) span.context();
            intrinsics.put("name", context.getTransactionState().getTransactionName());
            final DistributedTracing dt = DistributedTracing.getInstance();
            final DistributedTracingState distributedTracingState = context.getDistributedTracingState();
            intrinsics.putAll(dt.getDistributedTracingAttributes(distributedTracingState, context.getTransactionState().getTransactionId(), span.priority()));

            final DistributedTracePayloadImpl inboundPayload = distributedTracingState.getInboundPayload();
            if (inboundPayload != null && inboundPayload.hasTransactionId()) {
                //  TransactionEvents parent other TransactionEvents
                intrinsics.put("parentId", inboundPayload.getTransactionId());
                if (inboundPayload.hasGuid()) {
                    // Guid of span that started the TransactionEvent
                    intrinsics.put("parentSpanId", inboundPayload.getGuid());
                }
            }

            if (context.getTransactionState().hasError()) {
                intrinsics.put("error", true);
            }
        }

        userAttributes.putAll(span.getTags());
        userAttributes.remove("http.status_code");

        String status = parseStatusCode(span.getTag("http.status_code"));
        if (status != null && !status.isEmpty()) {
            agentAttributes.put("response.status", status);
        }
    }

    /**
     * New Relic expects status code to be a string, but the OpenTracing semantic convention is an integer,
     * so check for either, but return it as a string.
     */
    private String parseStatusCode(Object statusCodeObject) {
        if (statusCodeObject instanceof String) {
            return (String) statusCodeObject;
        } else if (statusCodeObject instanceof Number) {
            return ((Number) statusCodeObject) + "";
        }
        return null;
    }

    @Override
    public Map<String, Object> getIntrinsics() {
        return intrinsics;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return agentAttributes;
    }

}
