/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.state;

import com.newrelic.opentracing.util.DistributedTraceUtil;

import java.util.UUID;

public class TransactionState {

    private final long requestSalt;
    private final String transactionId;
    private volatile float transactionDuration = 0f;
    private volatile boolean error = false;
    private volatile String transactionName;

    public TransactionState() {
        this(null);
    }

    public TransactionState(Object awsRequestId) {
        this.requestSalt = toSalt(awsRequestId);
        this.transactionId = DistributedTraceUtil.generateGuid(requestSalt);
    }

    private static long toSalt(Object awsRequestId) {
        if (awsRequestId == null) {
            return 0L;
        }
        try {
            return UUID.fromString(awsRequestId.toString()).getLeastSignificantBits();
        } catch (IllegalArgumentException e) {
            return awsRequestId.toString().hashCode();
        }
    }

    public long getRequestSalt() {
        return requestSalt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionName(String transactionType, String functionName) {
        transactionName = transactionType + "/Function/" + functionName;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public float getTransactionDuration() {
        return transactionDuration;
    }

    public void setTransactionDuration(float transactionDuration) {
        this.transactionDuration = transactionDuration;
    }

    public void setError() {
        error = true;
    }

    public boolean hasError() {
        return error;
    }

}
