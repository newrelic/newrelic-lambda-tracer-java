/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.events;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;

import java.util.Arrays;
import java.util.Map;

public abstract class Event implements JSONAware {

    public abstract Map<String, Object> getIntrinsics();

    public abstract Map<String, Object> getUserAttributes();

    public abstract Map<String, Object> getAgentAttributes();

    /**
     * Print an event according to the following data format: an array of 3 hashes representing intrinsics,
     * user attributes, and agent attributes.
     */
    @Override
    public String toString() {
        return toJSONString();
    }

    @Override
    public String toJSONString() {
        return JSONArray.toJSONString(Arrays.asList(getIntrinsics(), getUserAttributes(), getAgentAttributes()));
    }

}
