/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.state;

import com.newrelic.opentracing.util.DistributedTraceUtil;

public class TransactionState {

    private final String transactionId = DistributedTraceUtil.generateGuid();
    private volatile float transactionDuration = 0f;
    private volatile boolean error = false;
    private volatile String transactionName;

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
