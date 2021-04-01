/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import io.opentracing.SpanContext;

import java.util.Map;

/**
 * Context that represents payload extracted by the tracer.
 */
public class LambdaPayloadContext implements SpanContext {

    private final DistributedTracePayloadImpl payload;
    private final long transportDurationInMillis;
    private final Map<String, String> baggage;

    public LambdaPayloadContext(DistributedTracePayloadImpl distributedTracePayload, long transportDurationInMillis, Map<String, String> baggage) {
        this.payload = distributedTracePayload;
        this.transportDurationInMillis = transportDurationInMillis;
        this.baggage = baggage;
    }

    public DistributedTracePayloadImpl getPayload() {
        return payload;
    }

    public long getTransportDurationInMillis() {
        return transportDurationInMillis;
    }

    public Map<String, String> getBaggage() {
        return baggage;
    }

    @Override
    public String toTraceId() {
        if (payload.hasTraceId()){
            return payload.getTraceId();
        }
        return "";
    }

    @Override
    public String toSpanId() {
        if (payload.hasGuid()){
            return payload.getGuid();
        }
        return "";
    }

    @Override
    public Iterable<Map.Entry<String, String>> baggageItems() {
        return baggage.entrySet();
    }

}
