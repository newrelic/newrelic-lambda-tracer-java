/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.dt;

/**
 * Payload used to connect two services in a distributed system.
 */
public interface DistributedTracePayload {

    /**
     * Get the distributed trace payload in JSON String format
     *
     * @return a JSON String representation of the payload
     */
    String text();

    /**
     * Get the distributed trace payload in base64 encoded JSON String format
     *
     * @return a base64 encoded JSON String representation of the payload
     */
    String httpSafe();

}
