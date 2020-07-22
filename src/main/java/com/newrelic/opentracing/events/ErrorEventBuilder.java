/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.events;

import java.util.HashMap;
import java.util.Map;

public class ErrorEventBuilder {

    private String errorClass;
    private String errorMessage;
    private String transactionName = "Unknown";
    private String transactionGuid;
    private long timestamp;
    private float transactionDuration;
    private Map<String, Object> userAttributes;
    private Map<String, Object> distributedTraceIntrinsics = new HashMap<>();

    public ErrorEventBuilder() {
    }

    public ErrorEventBuilder setTransactionName(String transactionName) {
        this.transactionName = transactionName;
        return this;
    }

    public ErrorEventBuilder setTransactionGuid(String transactionGuid) {
        this.transactionGuid = transactionGuid;
        return this;
    }

    public ErrorEventBuilder setDistributedTraceIntrinsics(Map<String, Object> distributedTraceIntrinsics) {
        this.distributedTraceIntrinsics = distributedTraceIntrinsics;
        return this;
    }

    public ErrorEventBuilder setErrorClass(String errorClass) {
        this.errorClass = errorClass;
        return this;
    }

    public ErrorEventBuilder setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public ErrorEventBuilder setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ErrorEventBuilder setTransactionDuration(float transactionDuration) {
        this.transactionDuration = transactionDuration;
        return this;
    }

    public ErrorEventBuilder setUserAttributes(Map<String, Object> tags) {
        this.userAttributes = tags;
        return this;
    }

    public ErrorEvent createError() {
        return new ErrorEvent(timestamp, transactionDuration, errorClass, errorMessage, transactionName,
                transactionGuid, userAttributes, distributedTraceIntrinsics);
    }

}