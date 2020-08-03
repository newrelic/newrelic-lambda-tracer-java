/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class TestContext implements Context {
    @Override
    public String getAwsRequestId() {
        return "de0ec083-0fab-4a9d-a940-a413b9af4cd4";
    }

    @Override
    public String getLogGroupName() {
        return "/aws/lambda/test";
    }

    @Override
    public String getLogStreamName() {
        return "2019/10/04/[$LATEST]a784a209393fc67707ccc87a0a4dbfd6";
    }

    @Override
    public String getFunctionName() {
        return "test";
    }

    @Override
    public String getFunctionVersion() {
        return "$LATEST";
    }

    @Override
    public String getInvokedFunctionArn() {
        return "arn:aws:lambda:us-west-2:000000000000:function:test";
    }

    @Override
    public CognitoIdentity getIdentity() {
        return new CognitoIdentity() {
            @Override
            public String getIdentityId() {
                return "";
            }

            @Override
            public String getIdentityPoolId() {
                return "";
            }
        };
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 0;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 0;
    }

    @Override
    public LambdaLogger getLogger() {
        return null;
    }
}
