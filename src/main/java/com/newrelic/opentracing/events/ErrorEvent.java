/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.events;

import java.util.HashMap;
import java.util.Map;

public class ErrorEvent extends Event {

    private static final String TYPE = "TransactionError";

    private final long timestamp;
    private final float transactionDuration;
    private final String errorClass;
    private final String errorMessage;
    private final Map<String, Object> userAttributes;

    private String transactionName;
    private String transactionGuid;
    private Map<String, Object> distributedTraceIntrinsics;

    ErrorEvent(long timestamp, float transactionDuration, String errorClass, String errorMessage, String transactionName, String transactionGuid,
            Map<String, Object> userAttributes, Map<String, Object> distributedTraceIntrinsics) {
        this.timestamp = timestamp;
        this.transactionDuration = transactionDuration;
        this.errorClass = errorClass;
        this.errorMessage = errorMessage;
        this.transactionName = transactionName;
        this.transactionGuid = transactionGuid;
        this.userAttributes = userAttributes;
        this.distributedTraceIntrinsics = distributedTraceIntrinsics;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> getIntrinsics() {
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("type", TYPE);
        intrinsics.put("error.class", errorClass);
        intrinsics.put("error.message", errorMessage);
        intrinsics.put("timestamp", timestamp);
        intrinsics.put("duration", transactionDuration);
        intrinsics.put("transactionName", transactionName);
        intrinsics.put("nr.transactionGuid", transactionGuid);
        intrinsics.putAll(distributedTraceIntrinsics);
        return intrinsics;
    }

}
