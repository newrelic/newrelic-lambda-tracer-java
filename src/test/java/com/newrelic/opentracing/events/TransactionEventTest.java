/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.SpanTestUtils;

import java.util.HashMap;
import java.util.Map;

import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.TransactionState;
import org.junit.jupiter.api.Test;

class TransactionEventTest {

    @Test
    void testTransactionEvent() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("world", "hello");
        tags.put("one", 1);
        tags.put("tagThree", "value");

        final LambdaSpan span = SpanTestUtils.createSpan("fetch", System.currentTimeMillis(), System.nanoTime(), tags, null, "guid");
        TransactionState txnState = new TransactionState();
        DistributedTracingState dtState = new DistributedTracingState();
        final TransactionEvent transactionEvent = new TransactionEvent(span, txnState, dtState);

        assertEquals("hello", transactionEvent.getUserAttributes().get("world"));
        assertEquals(1, transactionEvent.getUserAttributes().get("one"));
        assertEquals("value", transactionEvent.getUserAttributes().get("tagThree"));

        assertNotNull(transactionEvent.getIntrinsics().get("duration"));
        assertNotNull(transactionEvent.getIntrinsics().get("guid"));
        assertNotNull(transactionEvent.getIntrinsics().get("type"));
        assertNotNull(transactionEvent.getIntrinsics().get("sampled"));
        assertNotNull(transactionEvent.getIntrinsics().get("timestamp"));
        assertNotNull(transactionEvent.getIntrinsics().get("priority"));
    }

}
