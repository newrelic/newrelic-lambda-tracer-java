/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.traces;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorTrace implements JSONAware {

    private long timestamp;
    private String transactionName;
    private String message;
    private String errorType;
    private List<String> stackTrace;
    private Map<String, Object> intrinsics;
    private Map<String, Object> userAttributes;
    private String transactionGuid;

    ErrorTrace(long timestamp, String transactionName, String message, String errorType, List<String> stackTrace,
               Map<String, Object> intrinsics, Map<String, Object> userAttributes, String transactionGuid) {
        this.timestamp = timestamp;
        this.transactionName = transactionName;
        this.message = message;
        this.errorType = errorType;
        this.stackTrace = stackTrace;
        this.intrinsics = intrinsics;
        this.userAttributes = userAttributes;
        this.transactionGuid = transactionGuid;
    }

    private Map<String, Object> getAttributes() {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("stack_trace", stackTrace);
        attributes.put("agentAttributes", new HashMap<>());
        attributes.put("userAttributes", userAttributes);
        attributes.put("intrinsics", intrinsics);
        return attributes;
    }

    @Override
    public String toString() {
        return toJSONString();
    }

    @Override
    public String toJSONString() {
        return JSONArray.toJSONString(Arrays.asList(timestamp, transactionName, message, errorType, getAttributes(), transactionGuid));
    }

}
