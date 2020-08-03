/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.newrelic.opentracing.LambdaTracer;
import com.newrelic.opentracing.aws.TracingRequestHandler;
import io.opentracing.util.GlobalTracer;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * AWS Lambda function that utilizes the AWS Lambda OpenTracing SDK and New Relic AWS Lambda OpenTracing Tracer.
 * <p>
 * This function simply prints out the input and headers map it received from API Gateway when invoked.
 */
public class DTCalleeFunction implements TracingRequestHandler<Map<String, Object>, Map<String, Object>> {
    static {
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    private static final Logger LOG = Logger.getLogger(DTCalleeFunction.class);

    /**
     * Logs the input and headers map it received when invoked through API Gateway Proxy and returns an Response Map.
     *
     * @param input   Input received from API Gateway Proxy
     * @param context Lambda execution environment context object
     * @return Map of Response statusCode and body
     */
    @Override
    public Map<String, Object> doHandleRequest(Map<String, Object> input, Context context) {
        // Log Lambda function input details received from API Gateway to the Cloudwatch logs
        if (input != null && !input.isEmpty()) {
            LOG.info("Lambda function input: " + input);
            LOG.info("Headers: " + input.get("headers"));
        } else {
            LOG.info("Lambda function did not receive any input");
        }

        return new Response(200, "OK").getMap();
    }
}
