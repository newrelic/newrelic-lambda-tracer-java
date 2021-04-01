/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newrelic.TestLambdaCollector;
import com.newrelic.opentracing.*;
import com.newrelic.opentracing.dt.DistributedTracing.Configuration;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.state.DistributedTracingState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.newrelic.opentracing.state.TransactionState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


class DistributedTracingTest {

    @BeforeAll
    static void setup() {
        //when testing via gradle, use env vars, otherwise force the config.
        if (System.getenv("NEW_RELIC_ACCOUNT_ID") == null) {
            DistributedTracing.setConfiguration(new Configuration("trustKey", "account", "primaryApp"));
        }
    }

    @Test
    void testCreatePayloadFromSpan() {
        assertEquals("trustKey", DistributedTracing.getInstance().getTrustKey());
        assertEquals("account", DistributedTracing.getInstance().getAccountId());
        assertEquals("primaryApp", DistributedTracing.getInstance().getApplicationId());

        final LambdaSpan span = SpanTestUtils.createSpan("operation", System.currentTimeMillis(), System.nanoTime(), new HashMap<>(),
                null, "guid");

        final LambdaSpanContext context = (LambdaSpanContext) span.context();

        final DistributedTracing distributedTracing = DistributedTracing.getInstance();
        final DistributedTracePayloadImpl payload = distributedTracing.createDistributedTracePayload(span);
        assertTrue(payload.getTimestamp() > 0);
        assertEquals("App", payload.getParentType());
        assertEquals("account", payload.getAccountId());
        assertEquals("trustKey", payload.getTrustKey());
        assertEquals("primaryApp", payload.getApplicationId());
        assertEquals(span.guid(), payload.getGuid());
        assertEquals(((LambdaSpanContext) span.context()).getPriority(), payload.getPriority(), 0.0f);
        assertEquals(((LambdaSpanContext) span.context()).getTransactionId(), payload.getTransactionId());
    }

    @Test
    void testMajorMinorSupportedVersions() {
        assertEquals(1, DistributedTracing.getInstance().getMinorSupportedCatVersion());
        assertEquals(0, DistributedTracing.getInstance().getMajorSupportedCatVersion());
    }

    @Test
    void testIntrinsics() {
        final DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload("traceId", "guid", "txnId", 0.8f);
        LambdaPayloadContext lambdaPayloadContext = new LambdaPayloadContext(payload, 450, Collections.emptyMap());
        final DistributedTracingState dtState = new DistributedTracingState(lambdaPayloadContext);

        final Map<String, Object> dtAtts = DistributedTracing.getInstance().getDistributedTracingAttributes(dtState, "someOtherGuid", 1.4f);
        assertEquals("App", dtAtts.get("parent.type"));
        assertEquals("primaryApp", dtAtts.get("parent.app"));
        assertEquals("account", dtAtts.get("parent.account"));
        assertEquals("Unknown", dtAtts.get("parent.transportType"));
        assertEquals(0.45f, (Float) dtAtts.get("parent.transportDuration"), 0.0f);
        assertEquals("someOtherGuid", dtAtts.get("guid"));
        assertEquals("traceId", dtAtts.get("traceId"));
        assertEquals(1.4f, (Float) dtAtts.get("priority"), 0.0f);
    }

    @Test
    void parsePayload() {
        String strPayload = "{" +
                "  \"v\": [0,1]," +
                "  \"d\": {" +
                "    \"ty\": \"App\"," +
                "    \"ac\": \"account\"," +
                "    \"tk\": \"trustKey\"," +
                "    \"ap\": \"application\"," +
                "    \"id\": \"5f474d64b9cc9b2a\"," +
                "    \"tr\": \"3221bf09aa0bcf0d\"," +
                "    \"pr\": 0.1234," +
                "    \"sa\": false," +
                "    \"ti\": 1482959525577," +
                "    \"tx\": \"27856f70d3d314b7\"" +
                "  }" +
                "}";

        final DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.parseDistributedTracePayload(strPayload);
        assertNotNull(payload, "payload should not be null");
        assertEquals("trustKey", payload.getTrustKey());
        assertEquals("account", payload.getAccountId());
        assertEquals("application", payload.getApplicationId());
        assertEquals(0.1234f, payload.getPriority(), 0.0f);
        assertEquals("3221bf09aa0bcf0d", payload.getTraceId());
        assertFalse(payload.isSampled());
        assertEquals("App", payload.getParentType());
        assertEquals("5f474d64b9cc9b2a", payload.getGuid());
        assertEquals("27856f70d3d314b7", payload.getTransactionId());
    }

    @Test
    public void testInboundPayloadSpanParenting() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("world", "hello");
        tags.put("one", 1);
        tags.put("tagThree", "value");

        final LambdaSpan currentSpan = SpanTestUtils.createSpanWithInboundPayload("fetch", System.currentTimeMillis(), System.nanoTime(), tags, "guidCurrent", new LambdaCollector());

        final Map<String, Object> currentSpanIntrinsics = currentSpan.getIntrinsics();

        assertNotNull(currentSpanIntrinsics.get("traceId"));
        assertNotNull(currentSpanIntrinsics.get("priority"));
        assertNotNull(currentSpanIntrinsics.get("sampled"));
        assertNotNull(currentSpanIntrinsics.get("transactionId"));
        assertNotNull(currentSpanIntrinsics.get("parentId"));
        assertNotNull(currentSpanIntrinsics.get("parent.transportType"));
        assertNotNull(currentSpanIntrinsics.get("parent.transportDuration"));
        assertNotNull(currentSpanIntrinsics.get("sampled"));
        assertNotNull(currentSpanIntrinsics.get("guid"));

        assertEquals(1.2345f, currentSpanIntrinsics.get("priority"));
        assertEquals(true, currentSpanIntrinsics.get("sampled"));
        assertNotNull(currentSpanIntrinsics.get("transactionId"));
        assertEquals("guidInbound", currentSpanIntrinsics.get("parentId"));
        assertEquals("Unknown", currentSpanIntrinsics.get("parent.transportType"));
        assertEquals(1.337f, currentSpanIntrinsics.get("parent.transportDuration"));
    }

    @Test
    public void testInboundPayloadTransactionEventParenting() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("world", "hello");
        tags.put("one", 1);
        tags.put("tagThree", "value");

        final TestLambdaCollector collector = new TestLambdaCollector();
        final LambdaSpan currentSpan = SpanTestUtils.createSpanWithInboundPayload("fetch", System.currentTimeMillis(), System.nanoTime(), tags, "guidCurrent", collector);
        currentSpan.finish();

        final TransactionEvent transactionEvent = collector.getTxnEvent();
        final Map<String, Object> txnEventIntrinsics = transactionEvent.getIntrinsics();

        assertNotNull(txnEventIntrinsics.get("traceId"));
        assertNotNull(txnEventIntrinsics.get("priority"));
        assertNotNull(txnEventIntrinsics.get("sampled"));
        assertNotNull(txnEventIntrinsics.get("parentSpanId"));
        assertNotNull(txnEventIntrinsics.get("parentId"));
        assertNotNull(txnEventIntrinsics.get("name"));
        assertNotNull(txnEventIntrinsics.get("parent.transportType"));
        assertNotNull(txnEventIntrinsics.get("parent.transportDuration"));
        assertNotNull(txnEventIntrinsics.get("sampled"));

        assertEquals(1.2345f, txnEventIntrinsics.get("priority"));
        assertEquals(true, txnEventIntrinsics.get("sampled"));
        assertEquals("guidInbound", txnEventIntrinsics.get("parentSpanId"));
        assertNotNull(txnEventIntrinsics.get("parentId"));
        assertEquals("Other/Function/Test", txnEventIntrinsics.get("name"));
        assertEquals("Unknown", txnEventIntrinsics.get("parent.transportType"));
        assertEquals(1.337f, txnEventIntrinsics.get("parent.transportDuration"));
    }
}
