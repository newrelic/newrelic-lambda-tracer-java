/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.events.Event;
import com.newrelic.opentracing.util.SpanCategoryDetection;
import com.newrelic.opentracing.util.Stacktraces;
import com.newrelic.opentracing.util.TimeUtil;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.tag.Tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LambdaSpan extends Event implements Span {

    private LambdaSpanContext context;
    private long durationInMicros; // open tracing duration is micro-seconds
    private String operationName;

    private final String type;
    private final long startTimeInNanos; // used to compute accurate duration
    private final long timestamp; // start (epoch) time in milli-seconds
    private final String guid;
    private final String parentId;
    private final boolean isRootSpan;

    private final Map<String, Object> tags = new HashMap<>();
    private final Map<String, LogEntry> logs = new HashMap<>();
    private final Map<String, String> baggage = new HashMap<>();
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    LambdaSpan(String operationName, long timestamp, long startTimeInNanos, Map<String, Object> tags, LambdaSpan parentSpan, String guid) {
        this.type = "Span";
        this.operationName = operationName;
        this.timestamp = timestamp;
        this.startTimeInNanos = startTimeInNanos;
        if (tags != null) { this.tags.putAll(tags); }
        this.guid = guid;
        this.isRootSpan = parentSpan == null;
        this.parentId = parentSpan == null ? null : parentSpan.guid();
    }

    public String guid() {
        return guid;
    }

    public String getOperationName() {
        return operationName;
    }

    public long getDurationInMicros() {
        return durationInMicros;
    }

    public float getDurationInSeconds() {
        return durationInMicros / TimeUtil.MICROSECONDS_PER_SECOND;
    }

    public long getTimestamp() {
        return timestamp;
    }

    void setContext(LambdaSpanContext context) {
        this.context = context;
    }

    public boolean isRootSpan() {
        return isRootSpan;
    }

    @Override
    public SpanContext context() {
        return context;
    }

    public Object getTag(String key) {
        if (key == null) {
            return null;
        }
        return tags.get(key);
    }

    public LogEntry getLog(String eventName) {
        return logs.get(eventName);
    }

    @Override
    public Span setTag(String key, String value) {
        if (key != null && value != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public Span setTag(String key, boolean value) {
        if (key != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public Span setTag(String key, Number value) {
        if (key != null && value != null) {
            tags.put(key, value);
        }
        return this;
    }

    @Override
    public <T> Span setTag(Tag<T> tag, T value) {
        if (tag != null) {
            if (tag.getKey() != null && value != null) {
                tags.put(tag.getKey(), value);
            }
        }
        return this;
    }

    @Override
    public Span setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    @Override
    public Span log(Map<String, ?> fields) {
        return log(System.currentTimeMillis(), fields);
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        final long timeInMillis = timestampMicroseconds / TimeUtil.MICROSECONDS_PER_MILLISECOND;
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            log(timeInMillis, entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public Span log(String event) {
        return log(System.currentTimeMillis(), event);
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        return log(timestampMicroseconds / TimeUtil.MICROSECONDS_PER_MILLISECOND, "event", event);
    }

    private Span log(long timestampInMillis, String eventName, Object value) {
        if (value instanceof StackTraceElement[]) {
            value = Stacktraces.stackTracesToStrings((StackTraceElement[]) value);
        }

        if (value != null) {
            if ("event".equals(eventName) && "error".equals(value)) {
                context.setError();
            }
            logs.put(eventName, new LogEntry(timestampInMillis, value));
        }

        return this;
    }

    private long getEpochTimestampMicroseconds(long nanoTime) {
        // Calculate how many micros have elapsed since the start time
        long microsSinceStart = TimeUnit.NANOSECONDS.toMicros(nanoTime - startTimeInNanos);

        // Add micros since the start to start time to get a better estimate of current time
        return TimeUnit.NANOSECONDS.toMicros(startTimeInNanos) + microsSinceStart;
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        baggage.put(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return baggage.get(key);
    }

    @Override
    public void finish() {
        finish(getEpochTimestampMicroseconds(System.nanoTime()));
    }

    @Override
    public void finish(long finishMicros) {
        if (isFinished.compareAndSet(false, true)) {
            durationInMicros = finishMicros - TimeUnit.NANOSECONDS.toMicros(startTimeInNanos);
            recordTransactionInfo();
            context.collect();
        }
    }

    /**
     * Must be called before spanFinished.
     */
    private void recordTransactionInfo() {
        if (isRootSpan) {
            context.setTransactionDuration(getDurationInSeconds());

            String transactionType = "Other";

            Object eventSourceArnTag = getTag("aws.lambda.eventSource.arn");
            final String eventSourceArn = eventSourceArnTag instanceof String ? (String) eventSourceArnTag : "";
            if (eventSourceArn.startsWith("arn:aws:iam") || eventSourceArn.startsWith("arn:aws:elasticloadbalancing")) {
                transactionType = "WebTransaction";
            }

            final String arn = (String) getTag("aws.lambda.arn");
            if (arn != null && arn.contains(":")) {
                context.setTransactionName(transactionType, arn.substring(arn.lastIndexOf(":") + 1));
            }
        }
    }

    public Map<String, Object> getTags() {
        return filterNullMapEntries(tags);
    }

    public Map<String, Object> filterNullMapEntries(Map<String, Object> map) {
        map.keySet().removeAll(Collections.singleton(null));
        map.values().removeAll(Collections.singleton(null));
        return map;
    }

    public boolean isSampled() {
        return context.isSampled();
    }

    @Override
    public Map<String, Object> getIntrinsics() {
        final Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("type", type);
        intrinsics.put("name", operationName);
        intrinsics.put("timestamp", timestamp);
        intrinsics.put("duration", getDurationInSeconds()); // duration as float in seconds
        intrinsics.put("category", SpanCategoryDetection.detectSpanCategory(this).toString());

        if (isRootSpan) {
            intrinsics.put("nr.entryPoint", true);
        }

        // Get the parentId directly from the parent Span or inbound distributed tracing payload, if either exists.
        if (parentId != null && !parentId.isEmpty()) {
            intrinsics.put("parentId", parentId);
        } else if (context != null) {
            String contextParentId = context.getParentId();
            if (contextParentId != null) {
                intrinsics.put("parentId", contextParentId);
            }
        }

        if (context != null) {
            intrinsics.put("transactionId", context.getTransactionId());
            intrinsics.putAll(context.getDistributedTracingAttributes());
        }

        return intrinsics;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        Map<String, Object> userAtts = new HashMap<>();

        final Map<String, Object> tags = this.getTags();
        if (tags != null) {
            userAtts.putAll(tags);
        }

        userAtts.remove("error");
        return userAtts;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return new HashMap<>();
    }
}
