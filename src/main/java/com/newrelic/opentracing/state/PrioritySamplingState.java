/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.state;

import com.newrelic.opentracing.util.DistributedTraceUtil;

public class PrioritySamplingState {
    private final float priority;
    private final boolean sampled;

    public static PrioritySamplingState setSampledAndGeneratePriority(boolean computeSampled) {
        final float priority = DistributedTraceUtil.nextTruncatedFloat() + (computeSampled ? 1.0f : 0.0f);
        return new PrioritySamplingState(priority, computeSampled);
    }

    public PrioritySamplingState(float priority, boolean sampled) {
        this.priority = priority;
        this.sampled = sampled;
    }

    public float getPriority() {
        return priority;
    }

    public boolean isSampled() {
        return sampled;
    }
}
