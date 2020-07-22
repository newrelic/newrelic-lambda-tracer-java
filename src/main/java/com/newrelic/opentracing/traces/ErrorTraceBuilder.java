package com.newrelic.opentracing.traces;

import java.util.List;
import java.util.Map;

public class ErrorTraceBuilder {

    private long timestamp;
    private String transactionName;
    private String message;
    private String errorType;
    private List<String> stackTrace;
    private Map<String, Object> intrinsics;
    private Map<String, Object> userAttributes;
    private String transactionGuid;

    public ErrorTraceBuilder() {
    }

    public ErrorTraceBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ErrorTraceBuilder setTransactionName(String transactionName) {
        this.transactionName = transactionName;
        return this;
    }

    public ErrorTraceBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public ErrorTraceBuilder setErrorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public ErrorTraceBuilder setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    public ErrorTraceBuilder setIntrinsics(Map<String, Object> intrinsics) {
        this.intrinsics = intrinsics;
        return this;
    }

    public ErrorTraceBuilder setUserAttributes(Map<String, Object> userAttributes) {
        this.userAttributes = userAttributes;
        return this;
    }

    public ErrorTraceBuilder setTransactionGuid(String transactionGuid) {
        this.transactionGuid = transactionGuid;
        return this;
    }

    public ErrorTrace createErrorTrace() {
        return new ErrorTrace(timestamp, transactionName, message, errorType, stackTrace, intrinsics, userAttributes, transactionGuid);
    }

}