package com.newrelic.opentracing.state;

import com.newrelic.opentracing.util.DistributedTraceUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PrioritySamplingState {

    private AtomicReference<Float> priority = new AtomicReference<>(0.0f);
    private AtomicBoolean sampled = new AtomicBoolean(false);

    public void setPriority(float newPriority) {
        priority.set(newPriority);
    }

    public float getPriority() {
        return priority.get();
    }

    public void setSampled(boolean sampled) {
        this.sampled.set(sampled);
    }

    public boolean isSampled() {
        return sampled.get();
    }

    public void setSampledAndGeneratePriority(boolean computeSampled) {
        final float priority = DistributedTraceUtil.nextTruncatedFloat() + (computeSampled ? 1.0f : 0.0f);
        setSampled(computeSampled);
        setPriority(priority);
    }

}
