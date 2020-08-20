/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DTCalleeFunctionTest {
    private String NR_DT_HEADER_KEY = "newrelic";
    private String NR_DT_HEADER_PAYLOAD = "eyJkIjp7ImFjIjoiMTExIiwicHIiOjEuMTEyMTE1LCJ0eCI6IjEyNjVlNzM0ZWNiMjI2MjkiLCJ0aSI6MTU3MDE0NTA1OTY2MywidHkiOiJBcHAiLCJ0ayI6IjMzMyIsImlkIjoiNjZmZGI4ZGQ5Mzk1MTAxZiIsInRyIjoiZTg0NjNjMWQ1YzIxYzMwZCIsInNhIjp0cnVlLCJhcCI6IjIyMiJ9LCJ2IjpbMCwxXX0=";

    @BeforeClass
    public static void setup() {
        Map<String, String> environmentVars = new HashMap<>();
        environmentVars.put("NEW_RELIC_ACCOUNT_ID", "111");
        environmentVars.put("NEW_RELIC_TRUSTED_ACCOUNT_KEY", "333");
        environmentVars.put("NEW_RELIC_PRIMARY_APPLICATION_ID", "222");
        setEnvironmentVariables(environmentVars);
    }

    @Test
    public void successfulResponse() {
        /*
         * Parsed values from the compressed inbound payload:
         * {
         *   timestamp=1570145059663,
         *   parentType='App',
         *   accountId='111',
         *   trustKey='333',
         *   applicationId='222',
         *   guid='66fdb8dd9395101f',
         *   traceId='e8463c1d5c21c30d',
         *   txnId='1265e734ecb22629',
         *   sampled=true,
         *   priority=1.112115
         * }
         */
        Map<String, String> newrelicHeader = Collections.singletonMap(NR_DT_HEADER_KEY, NR_DT_HEADER_PAYLOAD);

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent().withHeaders(newrelicHeader);

        DTCalleeFunction calleeFunction = new DTCalleeFunction();
        final APIGatewayProxyResponseEvent response = calleeFunction.handleRequest(requestEvent, new TestContext());

        assertEquals(200, response.getStatusCode().intValue());
        assertEquals("OK", response.getBody());
    }

    private static void setEnvironmentVariables(Map<String, String> environmentVariables) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(environmentVariables);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(environmentVariables);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = null;
                    try {
                        field = cl.getDeclaredField("m");
                    } catch (NoSuchFieldException exception) {
                        throw new RuntimeException(exception);
                    }
                    field.setAccessible(true);
                    Object obj;
                    try {
                        obj = field.get(env);
                    } catch (IllegalAccessException exception) {
                        throw new RuntimeException(exception);
                    }
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(environmentVariables);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
