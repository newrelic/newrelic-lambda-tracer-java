package com.newrelic.opentracing.state;

import com.newrelic.opentracing.util.DistributedTraceUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class TransactionState {

    private final AtomicReference<String> transactionId = new AtomicReference<>(DistributedTraceUtil.generateGuid());
    private final AtomicReference<Float> transactionDuration = new AtomicReference<>(0.0f);
    private final AtomicBoolean error = new AtomicBoolean(false);

    private String transactionName;

    public String getTransactionId() {
        return transactionId.get();
    }

    public void setTransactionName(String transactionType, String functionName) {
        transactionName = transactionType + "/Function/" + functionName;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public float getTransactionDuration() {
        return transactionDuration.get();
    }

    public void setTransactionDuration(float transactionDuration) {
        this.transactionDuration.set(transactionDuration);
    }

    public void setError() {
        error.compareAndSet(false, true);
    }

    public boolean hasError() {
        return error.get();
    }

}
