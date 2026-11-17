/*
 * Copyright 2026 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newrelic.GlobalTracerTestUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LambdaSpanBuilderTest {

    @BeforeAll
    static void beforeClass() {
        GlobalTracerTestUtils.initTracer(LambdaTracer.INSTANCE);
    }

    @Test
    void rootSpan_derivesRequestSaltFromAwsRequestIdTag() {
        String awsRequestId = "550e8400-e29b-41d4-a716-446655440000";
        long expectedSalt = UUID.fromString(awsRequestId).getLeastSignificantBits();

        Span span = GlobalTracer.get().buildSpan("handleRequest")
                .withTag("aws.requestId", awsRequestId)
                .start();
        try {
            LambdaSpanContext context = (LambdaSpanContext) span.context();
            assertEquals(expectedSalt, context.getRequestSalt());
        } finally {
            span.finish();
        }
    }

    @Test
    void rootSpan_withoutAwsRequestIdTag_fallsBackToZeroSalt() {
        Span span = GlobalTracer.get().buildSpan("handleRequest").start();
        try {
            LambdaSpanContext context = (LambdaSpanContext) span.context();
            assertEquals(0L, context.getRequestSalt());
        } finally {
            span.finish();
        }
    }

    @Test
    void childSpan_inheritsParentsRequestSalt() {
        String awsRequestId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
        long expectedSalt = UUID.fromString(awsRequestId).getLeastSignificantBits();

        Span rootSpan = GlobalTracer.get().buildSpan("handleRequest")
                .withTag("aws.requestId", awsRequestId)
                .start();
        try (Scope scope = GlobalTracer.get().activateSpan(rootSpan)) {
            Span childSpan = GlobalTracer.get().buildSpan("childOperation").start();
            try {
                LambdaSpanContext childContext = (LambdaSpanContext) childSpan.context();
                assertEquals(expectedSalt, childContext.getRequestSalt());
            } finally {
                childSpan.finish();
            }
        } finally {
            rootSpan.finish();
        }
    }

}
