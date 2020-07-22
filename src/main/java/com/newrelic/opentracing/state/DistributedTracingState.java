package com.newrelic.opentracing.state;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.dt.DistributedTracePayload;
import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.util.DistributedTraceUtil;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DistributedTracingState {

    private AtomicReference<DistributedTracePayloadImpl> inboundPayload = new AtomicReference<>();
    private AtomicLong transportTimeMillis = new AtomicLong(Long.MIN_VALUE);
    private AtomicBoolean hasOutboundPayload = new AtomicBoolean(false);
    private AtomicReference<DistributedTracePayloadImpl> firstOutboundPayload = new AtomicReference<>();
    private AtomicReference<String> traceId = new AtomicReference<>(null);
    private Map<String, String> baggage;

    public DistributedTracePayloadImpl getInboundPayload() {
        return inboundPayload.get();
    }

    public void setInboundPayloadAndTransportTime(DistributedTracePayloadImpl payload, long transportTimeInMillis) {
        inboundPayload.compareAndSet(null, payload);
        transportTimeMillis.compareAndSet(Long.MIN_VALUE, transportTimeInMillis);
    }

    private void setOutboundPayload(DistributedTracePayloadImpl outboundPayload) {
        firstOutboundPayload.compareAndSet(null, outboundPayload);
        hasOutboundPayload.compareAndSet(false, true);
    }

    public boolean outboundPayloadCreated() {
        return hasOutboundPayload.get();
    }

    public String getTraceId() {
        final DistributedTracePayloadImpl payload = inboundPayload.get();
        final DistributedTracePayloadImpl firstOutboundPayload = this.firstOutboundPayload.get();

        if (payload != null) {
            return payload.getTraceId();
        } else if (firstOutboundPayload != null) {
            return firstOutboundPayload.getTraceId();
        }

        // Generated when request starts
        return traceId.get();
    }

    public void generateAndStoreTraceId() {
        traceId.set(DistributedTraceUtil.generateGuid());
    }

    public DistributedTracePayload createDistributedTracingPayload(LambdaSpan span) {
        final DistributedTracePayloadImpl outboundPayload = DistributedTracing.getInstance().createDistributedTracePayload(span);
        setOutboundPayload(outboundPayload);
        return outboundPayload;
    }

    public void setBaggage(Map<String, String> baggage) {
        this.baggage = baggage;
    }

    public Map<String, String> getBaggage() {
        return baggage;
    }

    /**
     * @return transport duration or Long.MIN_VALUE if not set
     */
    public long getTransportTimeMillis() {
        return transportTimeMillis.get();
    }

}
