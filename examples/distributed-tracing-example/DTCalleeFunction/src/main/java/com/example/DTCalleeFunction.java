/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.newrelic.opentracing.LambdaTracer;
import com.newrelic.opentracing.aws.LambdaTracing;
import io.opentracing.util.GlobalTracer;
import org.apache.log4j.Logger;

/**
 * AWS Lambda function that utilizes the AWS Lambda OpenTracing SDK and New Relic AWS Lambda OpenTracing Tracer.
 * <p>
 * This function simply prints out the input and headers map it received from API Gateway when invoked.
 */
public class DTCalleeFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static {
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    private static final Logger LOG = Logger.getLogger(DTCalleeFunction.class);

    /**
     * Logs the input and headers map it received when invoked through API Gateway Proxy and returns an Response Map.
     *
     * @param event   Input received from API Gateway Proxy
     * @param ctx Lambda execution environment context object
     * @return Map of Response statusCode and body
     */
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context ctx) {
        return LambdaTracing.instrument(event, ctx, (input, context) -> {
            // Log Lambda function input details received from API Gateway to the Cloudwatch logs
            if (input != null) {
                LOG.info("Lambda function input: " + input);
                LOG.info("Headers: " + input.getHeaders());
            } else {
                LOG.info("Lambda function did not receive any input");
            }
            APIGatewayProxyResponseEvent ret = new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("OK");

            return ret;
        });
    }
}
