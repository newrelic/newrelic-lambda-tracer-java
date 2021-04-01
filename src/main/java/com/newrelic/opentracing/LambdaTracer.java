/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayload;
import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.logging.Log;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.util.ThreadLocalScopeManager;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LambdaTracer implements Tracer {

    private static final String NEWRELIC_TRACE_HEADER = "newrelic";
    public static final LambdaTracer INSTANCE = new LambdaTracer();

    private final ScopeManager scopeManager = new ThreadLocalScopeManager();
    private final AdaptiveSampling adaptiveSampling = new AdaptiveSampling();

    private LambdaTracer() {
    }

    @Override
    public ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        return scopeManager.activeSpan();
    }

    @Override
    public Scope activateSpan(Span span) {
        return scopeManager.activate(span);
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new LambdaSpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (!(spanContext instanceof LambdaSpanContext)) {
            return;
        }

        LambdaSpanContext lambdaSpanContext = (LambdaSpanContext) spanContext;
        DistributedTracePayload distributedTracePayload = lambdaSpanContext.createDistributedTracingPayload();

        if (distributedTracePayload == null) {
            return;
        }

        if (format.equals(Format.Builtin.TEXT_MAP)) {
            ((TextMap) carrier).put(NEWRELIC_TRACE_HEADER, distributedTracePayload.text());
        } else if (format.equals(Format.Builtin.HTTP_HEADERS)) {
            ((TextMap) carrier).put(NEWRELIC_TRACE_HEADER, distributedTracePayload.httpSafe());
        } else if (format.equals(Format.Builtin.BINARY)) {
            // First, specify length of distributed trace payload as an index.
            byte[] payloadBytes = distributedTracePayload.text().getBytes(UTF_8);
            ((ByteBuffer) carrier).putInt(payloadBytes.length);
            ((ByteBuffer) carrier).put(payloadBytes);
        }
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        String payload = getPayloadString(format, carrier);
        if (payload == null) {
            return null;
        }

        DistributedTracePayloadImpl distributedTracePayload = DistributedTracePayloadImpl.parseDistributedTracePayload(payload);
        if (distributedTracePayload == null) {
            String msg = MessageFormat.format("{0} header value was not accepted.", NEWRELIC_TRACE_HEADER);
            Log.getInstance().debug(msg);
            throw new IllegalArgumentException(msg);
        }

        long transportDurationInMillis = Math.max(0, System.currentTimeMillis() - distributedTracePayload.getTimestamp());
        return new LambdaPayloadContext(distributedTracePayload, transportDurationInMillis, Collections.emptyMap());
    }

    @Override
    public void close() {
        // No-op
    }

    private <C> String getPayloadString(Format<C> format, C carrier) {
        String payload = null;
        if (format.equals(Format.Builtin.TEXT_MAP)) {
            for (Map.Entry<String, String> entry : ((TextMap) carrier)) {
                if (entry.getKey().equalsIgnoreCase(NEWRELIC_TRACE_HEADER)) {
                    payload = entry.getValue();
                }
            }
        } else if (format.equals(Format.Builtin.HTTP_HEADERS)) {
            if (((TextMap) carrier).iterator() == null) {
                throw new IllegalArgumentException("Invalid carrier.");
            }

            for (Map.Entry<String, String> entry : ((TextMap) carrier)) {
                if (entry.getKey().equalsIgnoreCase(NEWRELIC_TRACE_HEADER)) {
                    payload = new String(Base64.getDecoder().decode(entry.getValue()), UTF_8);
                }
            }
        } else if (format.equals(Format.Builtin.BINARY)) {
            ByteBuffer buffer = (ByteBuffer) carrier;
            if (buffer == null) {
                throw new IllegalArgumentException("Invalid carrier.");
            }

            int payloadLength = buffer.getInt();
            byte[] payloadBytes = new byte[payloadLength];
            buffer.get(payloadBytes);
            payload = new String(payloadBytes, UTF_8);
        } else {
            String msg = MessageFormat.format("Invalid or missing extract format: {0}.", format);
            Log.getInstance().debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (payload == null) {
            Log.getInstance().debug(MessageFormat.format("Unable to extract payload from carrier: {0}.", carrier));
            return null;
        }
        return payload;
    }

    AdaptiveSampling adaptiveSampling() {
        return adaptiveSampling;
    }

}
