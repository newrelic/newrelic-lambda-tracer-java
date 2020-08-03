/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.newrelic.opentracing.LambdaTracer;
import com.newrelic.opentracing.aws.TracingRequestHandler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.util.GlobalTracer;
import kong.unirest.GetRequest;
import kong.unirest.Header;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS Lambda function that utilizes the AWS Lambda OpenTracing SDK and New Relic AWS Lambda OpenTracing Tracer.
 * <p>
 * This function makes an external call to an API Gateway Proxy to invoke another AWS lambda function.
 */
public class DTCallerFunction implements TracingRequestHandler<Map<String, Object>, APIGatewayProxyResponseEvent> {
    static {
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    private static final Logger LOG = Logger.getLogger(DTCallerFunction.class);
    private static final String API_GATEWAY_PROXY_URL = "API_GATEWAY_PROXY_URL";
    private final Tracer tracer = GlobalTracer.get();

    /**
     * Invokes another AWS Lambda function through a call to API Gateway Proxy, logs the tracer results to the Lambda
     * Cloudwatch logs, and returns an APIGatewayProxyResponseEvent.
     *
     * @param input   Input received from API Gateway Proxy
     * @param context Lambda execution environment context object
     * @return APIGatewayProxyResponseEvent Response received from API Gateway Proxy
     */
    @Override
    public APIGatewayProxyResponseEvent doHandleRequest(Map<String, Object> input, Context context) {
        final HttpResponse<String> response;

        // Log Lambda function input details received from API Gateway to the Cloudwatch logs
        if (input != null && !input.isEmpty()) {
            LOG.info("Lambda function input: " + input);
            LOG.info("Headers: " + input.get("headers"));
        } else {
            LOG.info("Lambda function did not receive any input");
        }

        final String dtCalleeApiGatewayProxyURL = System.getenv(API_GATEWAY_PROXY_URL);

        if (dtCalleeApiGatewayProxyURL == null) {
            LOG.error("The API_GATEWAY_PROXY_URL environment variable must be set to properly invoke DTCalleeLambda");
            return new APIGatewayProxyResponseEvent()
                    .withBody("Bad Request: The API_GATEWAY_PROXY_URL environment variable must be set to properly invoke DTCalleeLambda")
                    .withStatusCode(400);
        }
        response = makeExternalCallToApiGateway(dtCalleeApiGatewayProxyURL);

        return new APIGatewayProxyResponseEvent()
                .withBody(response.getBody())
                .withStatusCode(response.getStatus())
                .withHeaders(getResponseHeaderMap(response));
    }

    /**
     * Makes an external request to an AWS API Gateway Proxy URL to invoke another lambda function. Attaches the
     * "newrelic" distributed tracing headers to the outgoing request if they exist.
     *
     * @param dtCalleeApiGatewayProxyURL URL for API Gateway Proxy endpoint to invoke DTCalleeLambda
     * @return HttpResponse Object resulting from the request
     */
    private HttpResponse<String> makeExternalCallToApiGateway(String dtCalleeApiGatewayProxyURL) {
        HttpResponse<String> response;
        final Map<String, String> distributedTracingHeaders = new HashMap<>();

        // Create a Span to trace this method
        final Span externalCallSpan = GlobalTracer.get().buildSpan("makeExternalCallToApiGatewayProxy").start();
        try (Scope scope = GlobalTracer.get().activateSpan(externalCallSpan)) {
            GetRequest request = Unirest.get(dtCalleeApiGatewayProxyURL);

            /*
             * It is important to get the last active span before calling tracer.inject below so that its context gets
             * propagated in the "newrelic" DT headers and the DT UI can do proper span parenting.
             */
            final Span activeSpan = tracer.activeSpan();

            /*
             * This call to inject will populate the distributedTracingHeaders map with "newrelic" DT headers of the form:
             * "newrelic" -> "eyJkIjp7ImFjIjoiMjIxMjg2NCIsInByIjoxLjkyMjkxNTksInR4IjoiNjNlOTIz..."
             * The DT headers can then easily be attached as headers to an outgoing request.
             */
            tracer.inject(activeSpan.context(), Format.Builtin.HTTP_HEADERS, new TextMapAdapter(distributedTracingHeaders));

            // Attach the "newrelic" DT headers to the outgoing request if they exist
            if (distributedTracingHeaders.containsKey("newrelic")) {
                request.header("newrelic", distributedTracingHeaders.get("newrelic"));
                externalCallSpan.setTag("newrelicHeaders", true);
            }

            // Request is fired off when asString() is called
            response = request.header("accept", "application/json").asString();

            // Add interesting attributes to the Span tracing this method
            externalCallSpan.setTag("externalUrl", dtCalleeApiGatewayProxyURL);
            externalCallSpan.setTag("responseMessage", response.getStatusText());
            externalCallSpan.setTag("responseCode", response.getStatus());
        } finally {
            externalCallSpan.finish();
        }

        return response;
    }

    /**
     * Converts the List of Header objects from the HttpResponse into a Map
     *
     * @param response HttpResponse
     * @return Map of headers
     */
    private Map<String, String> getResponseHeaderMap(HttpResponse response) {
        final List<Header> headerList = response.getHeaders().all();
        final Map<String, String> headerMap = new HashMap<>();
        headerList.forEach((header) -> headerMap.put(header.getName(), header.getValue()));
        return headerMap;
    }
}
