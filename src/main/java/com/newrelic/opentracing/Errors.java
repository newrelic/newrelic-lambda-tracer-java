/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracing;
import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.ErrorEventBuilder;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.state.TransactionState;
import com.newrelic.opentracing.traces.ErrorTrace;
import com.newrelic.opentracing.traces.ErrorTraceBuilder;
import com.newrelic.opentracing.util.Stacktraces;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class Errors {

    private final AtomicReference<List<ErrorEvent>> errorEvents = new AtomicReference<>(new LinkedList<>());
    private final AtomicReference<List<ErrorTrace>> errorTraces = new AtomicReference<>(new LinkedList<>());

    void recordErrors(LambdaSpan span) {
        // Need an error.object to record events and traces
        LogEntry errorObject = span.getLog("error.object");
        if (errorObject == null) {
            return;
        }

        // Need an error message to record events and traces
        // May use error.object as fallback, as long as object is a throwable
        LogEntry errorMessage = span.getLog("message");
        boolean errorObjectIsThrowable = errorObject.getValue() instanceof Throwable;
        if (errorMessage == null && !errorObjectIsThrowable) {
            return; // no error message and can't use Throwable to get message
        }

        String errorClass = errorObject.getValue().getClass().getName();
        String msg = getErrorMessage(errorMessage, errorObject);
        Map<String, Object> userAttributes = additionalAttributes(span);

        final TransactionState txnState = span.getTransactionState();
        final Map<String, Object> dtIntrinsics = getDistributedTracingIntrinsics(span, txnState);

        final ErrorEvent error = new ErrorEventBuilder()
                .setDistributedTraceIntrinsics(dtIntrinsics)
                .setErrorClass(errorClass)
                .setErrorMessage(msg)
                .setTransactionDuration(txnState.getTransactionDuration())
                .setTimestamp(errorObject.getTimestampInMillis())
                .setUserAttributes(userAttributes)
                .setTransactionName(txnState.getTransactionName())
                .setTransactionGuid(txnState.getTransactionId())
                .createError();
        errorEvents.get().add(error);

        // Need a stack trace to record a traced error
        LogEntry errorStack = span.getLog("stack");
        if (errorStack == null && !errorObjectIsThrowable) {
            return;
        }

        List<String> stackTrace = getStackTrace(errorStack, errorObject);
        final ErrorTrace errorTrace = new ErrorTraceBuilder()
                .setMessage(msg)
                .setErrorType(errorClass)
                .setTransactionGuid(txnState.getTransactionId())
                .setTransactionName(txnState.getTransactionName())
                .setUserAttributes(userAttributes)
                .setIntrinsics(dtIntrinsics)
                .setTimestamp(errorObject.getTimestampInMillis())
                .setStackTrace(stackTrace)
                .createErrorTrace();
        errorTraces.get().add(errorTrace);
    }

    private Map<String, Object> additionalAttributes(LambdaSpan span) {
        final Map<String, Object> attributes = new HashMap<>();
        LogEntry errorKind = span.getLog("error.kind");
        if (errorKind != null) {
            attributes.put("error.kind", errorKind.getValue());
        }
        return attributes;
    }

    private Map<String, Object> getDistributedTracingIntrinsics(LambdaSpan span, TransactionState txnState) {
        final DistributedTracingState dtState = span.getDistributedTracingState();
        final PrioritySamplingState priorityState = span.getPrioritySamplingState();
        return DistributedTracing.getInstance().getDistributedTracingAttributes(dtState, txnState.getTransactionId(), priorityState.getPriority());
    }

    private List<String> getStackTrace(LogEntry errorStack, LogEntry errorObject) {
        if (errorStack != null && errorStack.getValue() instanceof List) {
            return (List) errorStack.getValue();
        } else if (errorObject != null && errorObject.getValue() instanceof Throwable) {
            final StackTraceElement[] stackTrace = ((Throwable) errorObject.getValue()).getStackTrace();
            return Stacktraces.stackTracesToStrings(stackTrace);
        }
        return new LinkedList<>();
    }

    private String getErrorMessage(LogEntry errorMessage, LogEntry errorObject) {
        if (errorMessage != null) {
            return errorMessage.getValue().toString();
        }

        if (errorObject.getValue() instanceof Throwable) {
            return ((Throwable) errorObject.getValue()).getMessage();
        }

        return null;
    }

    List<ErrorTrace> getAndClearTraces() {
        return errorTraces.getAndSet(new LinkedList<>());
    }

    List<ErrorEvent> getAndClearEvents() {
        return errorEvents.getAndSet(new LinkedList<>());
    }

}
