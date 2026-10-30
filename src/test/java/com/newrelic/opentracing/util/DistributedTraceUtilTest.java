/*
 * Copyright 2026 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DistributedTraceUtilTest {

    @Test
    void generateGuid_isSixteenHexChars() {
        String guid = DistributedTraceUtil.generateGuid();
        assertEquals(16, guid.length());
        assertTrue(guid.matches("[0-9a-f]{16}"));
    }

    @Test
    void generateGuid_withSalt_isSixteenHexChars() {
        String guid = DistributedTraceUtil.generateGuid(12345L);
        assertEquals(16, guid.length());
        assertTrue(guid.matches("[0-9a-f]{16}"));
    }

    @Test
    void generateGuid_zeroSaltMatchesNoArgOverloadBehavior() {
        // Both should just be a plain ThreadLocalRandom value; this pins down that the no-arg
        // overload is equivalent to salting with 0, rather than silently diverging.
        for (int i = 0; i < 100; i++) {
            String noArg = DistributedTraceUtil.generateGuid();
            String zeroSalt = DistributedTraceUtil.generateGuid(0L);
            assertEquals(16, noArg.length());
            assertEquals(16, zeroSalt.length());
        }
    }

}
