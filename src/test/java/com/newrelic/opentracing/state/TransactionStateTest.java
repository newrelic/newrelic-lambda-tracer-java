/*
 * Copyright 2026 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TransactionStateTest {

    @Test
    void noArgConstructor_hasZeroRequestSalt() {
        assertEquals(0L, new TransactionState().getRequestSalt());
    }

    @Test
    void nullAwsRequestId_hasZeroRequestSalt() {
        assertEquals(0L, new TransactionState(null).getRequestSalt());
    }

    @Test
    void awsRequestId_isDerivedFromTheUuidBits() {
        String awsRequestId = "550e8400-e29b-41d4-a716-446655440000";
        long expectedSalt = UUID.fromString(awsRequestId).getLeastSignificantBits();

        assertEquals(expectedSalt, new TransactionState(awsRequestId).getRequestSalt());
    }

    @Test
    void differentAwsRequestIds_produceDifferentSalts() {
        long saltA = new TransactionState("550e8400-e29b-41d4-a716-446655440000").getRequestSalt();
        long saltB = new TransactionState("6ba7b810-9dad-11d1-80b4-00c04fd430c8").getRequestSalt();

        assertNotEquals(saltA, saltB);
    }

    @Test
    void sameAwsRequestId_producesTheSameSalt() {
        String awsRequestId = "550e8400-e29b-41d4-a716-446655440000";

        long saltA = new TransactionState(awsRequestId).getRequestSalt();
        long saltB = new TransactionState(awsRequestId).getRequestSalt();

        assertEquals(saltA, saltB);
    }

    @Test
    void malformedAwsRequestId_fallsBackWithoutThrowing() {
        TransactionState state = new TransactionState("not-a-uuid");
        assertNotEquals(0L, state.getRequestSalt());
    }

}
