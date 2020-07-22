package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.events.Event;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
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
import java.util.concurrent.atomic.AtomicReference;

public class LambdaSpan extends Event implements Span {

    private LambdaSpanContext context;
    private long durationInMicros; // open tracing duration is micro-seconds
    private String operationName;

    private final String type;
    private final long startTimeInNanos; // used to compute accurate duration
    private final long timestamp; // start (epoch) time in milli-seconds
    private final String guid;
    private final String transactionId;
    private final String parentId;
    private final boolean isRootSpan;

    private final AtomicReference<Map<String, Object>> tags = new AtomicReference<>(new HashMap<>());
    private final AtomicReference<Map<String, LogEntry>> logs = new AtomicReference<>(new HashMap<>());
    private final AtomicReference<Map<String, String>> baggage = new AtomicReference<>(new HashMap<>());
    private final AtomicBoolean isFinished = new AtomicBoolean(false);

    public LambdaSpan(String operationName, long timestamp, long startTimeInNanos, Map<String, Object> tags, LambdaSpan parentSpan, String guid,
            String transactionId) {
        this.type = "Span";
        this.operationName = operationName;
        this.timestamp = timestamp;
        this.startTimeInNanos = startTimeInNanos;
        if (tags != null) { this.tags.set(tags); }
        this.guid = guid;
        this.transactionId = transactionId;
        this.isRootSpan = parentSpan == null;
        this.parentId = parentSpan == null ? null : parentSpan.guid();
    }

    public float priority() {
        return context.getPrioritySamplingState().getPriority();
    }

    public String guid() {
        return guid;
    }

    String getOperationName() {
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

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public SpanContext context() {
        return context;
    }

    public Object getTag(String key) {
        if (key == null) {
            return null;
        }
        return tags.get().get(key);
    }

    public LogEntry getLog(String eventName) {
        return logs.get().get(eventName);
    }

    @Override
    public Span setTag(String key, String value) {
        if (key != null && value != null) {
            tags.get().put(key, value);
        }
        return this;
    }

    @Override
    public Span setTag(String key, boolean value) {
        if (key != null) {
            tags.get().put(key, value);
        }
        return this;
    }

    @Override
    public Span setTag(String key, Number value) {
        if (key != null && value != null) {
            tags.get().put(key, value);
        }
        return this;
    }

    @Override
    public <T> Span setTag(Tag<T> tag, T value) {
        if (tag != null) {
            if (tag.getKey() != null && value != null) {
                tags.get().put(tag.getKey(), value);
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
                getTransactionState().setError();
            }
            logs.get().put(eventName, new LogEntry(timestampInMillis, value));
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
        baggage.get().put(key, value);
        return this;
    }

    @Override
    public String getBaggageItem(String key) {
        return baggage.get() == null ? null : baggage.get().get(key);
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
            context.spanFinished(this);
            resetContext();
        }
    }

    /**
     * Must be called before spanFinished.
     */
    private void recordTransactionInfo() {
        if (isRootSpan) {
            context.getTransactionState().setTransactionDuration(getDurationInSeconds());

            String transactionType = "Other";

            Object eventSourceArnTag = getTag("aws.lambda.eventSource.arn");
            final String eventSourceArn = eventSourceArnTag instanceof String ? (String) eventSourceArnTag : "";
            if (eventSourceArn.startsWith("arn:aws:iam") || eventSourceArn.startsWith("arn:aws:elasticloadbalancing")) {
                transactionType = "WebTransaction";
            }

            final String arn = (String) getTag("aws.lambda.arn");
            if (arn != null && arn.contains(":")) {
                context.getTransactionState().setTransactionName(transactionType, arn.substring(arn.lastIndexOf(":") + 1));
            }
        }
    }

    /**
     * Must be called after spanFinished.
     */
    private void resetContext() {
        if (isRootSpan) {
            context.getScopeManager().resetState();
        }
    }

    public String traceId() {
        return context.getDistributedTracingState().getTraceId();
    }

    public Map<String, Object> getTags() {
        return filterNullMapEntries(tags.get());
    }

    public Map<String, Object> filterNullMapEntries(Map<String, Object> map) {
        map.keySet().removeAll(Collections.singleton(null));
        map.values().removeAll(Collections.singleton(null));
        return map;
    }

    DistributedTracingState getDistributedTracingState() {
        return context.getDistributedTracingState();
    }

    PrioritySamplingState getPrioritySamplingState() {
        return context.getPrioritySamplingState();
    }

    TransactionState getTransactionState() {
        return context.getTransactionState();
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
            final DistributedTracingState distributedTracingState = context.getDistributedTracingState();
            if (distributedTracingState != null) {
                final DistributedTracePayloadImpl inboundPayload = distributedTracingState.getInboundPayload();
                if (inboundPayload != null && inboundPayload.hasGuid()) {
                    intrinsics.put("parentId", inboundPayload.getGuid());
                }
            }
        }

        if (transactionId != null && !transactionId.isEmpty()) {
            intrinsics.put("transactionId", transactionId);
        }

        if (context != null) {
            final DistributedTracing dt = DistributedTracing.getInstance();
            final DistributedTracingState state = context.getDistributedTracingState();
            intrinsics.putAll(dt.getDistributedTracingAttributes(state, guid(), priority()));
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
