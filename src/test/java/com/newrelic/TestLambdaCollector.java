package com.newrelic;

import com.newrelic.opentracing.LambdaCollector;
import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.traces.ErrorTrace;

import java.util.List;

public class TestLambdaCollector extends LambdaCollector {
    private String arn;
    private List<LambdaSpan> spans;
    private TransactionEvent txnEvent;
    private List<ErrorEvent> errorEvents;
    private List<ErrorTrace> errorTraces;

    public String getArn() {
        return arn;
    }

    public List<LambdaSpan> getSpans() {
        return spans;
    }

    public TransactionEvent getTxnEvent() {
        return txnEvent;
    }

    public List<ErrorEvent> getErrorEvents() {
        return errorEvents;
    }

    public List<ErrorTrace> getErrorTraces() {
        return errorTraces;
    }

    @Override
    protected void writeData(String arn, List<LambdaSpan> spans, TransactionEvent txnEvent, List<ErrorEvent> errorEvents, List<ErrorTrace> errorTraces) {
        this.arn = arn;
        this.spans = spans;
        this.txnEvent = txnEvent;
        this.errorEvents = errorEvents;
        this.errorTraces = errorTraces;

        super.writeData(arn, spans, txnEvent, errorEvents, errorTraces);
    }
}
