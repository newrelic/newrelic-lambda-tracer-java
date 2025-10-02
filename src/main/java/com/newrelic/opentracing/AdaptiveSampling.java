/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class AdaptiveSampling {

    private final int target = 10;
    private final int samplingTargetPeriodInSeconds = 60;
    private final long samplingTargetPeriodInMilliSeconds = TimeUnit.SECONDS.toMillis(samplingTargetPeriodInSeconds);

    private final AtomicLong sampledTrueCount = new AtomicLong(0);
    private final AtomicLong decidedCount = new AtomicLong(0);
    private final AtomicLong decidedCountLast = new AtomicLong(0);

    private volatile long lastStart = 0;
    private volatile boolean firstPeriod = true;

    void reset() {
        lastStart = System.currentTimeMillis();
        firstPeriod = false;
        sampledTrueCount.set(0);
        decidedCountLast.set(decidedCount.get());
        decidedCount.set(0);
    }

    /**
     * Compute if a request should be marked as "sampled"
     *
     * Don't call this method if request has an inbound DT payload
     *
     * @return true if we should mark this request as sampled. False otherwise.
     */
    boolean computeSampled() {
        boolean sampled;

        if (firstPeriod) {
            sampled = sampledTrueCount.get() < target;
        } else if (sampledTrueCount.get() < target) {
            final long count = decidedCountLast.get();
            sampled = count == 0 || ThreadLocalRandom.current().nextLong(count) < target;
        } else {
            final double expTarget = Math.pow(target, (target * 1.0f / sampledTrueCount.get())) - Math.sqrt(target);
            sampled = ThreadLocalRandom.current().nextLong(decidedCount.get()) < expTarget;
        }

        decidedCount.incrementAndGet();

        if (sampled) {
            sampledTrueCount.incrementAndGet();
        }

        return sampled;
    }

    void requestStarted() {
        if (lastStart <= 0) {
            lastStart = System.currentTimeMillis();
        }

        final long now = System.currentTimeMillis();
        if (now >= lastStart + samplingTargetPeriodInMilliSeconds) {
            reset();
        }
    }

    int getTarget() {
        return target;
    }

}
