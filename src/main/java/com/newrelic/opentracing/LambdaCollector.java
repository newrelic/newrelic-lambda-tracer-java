/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing;

import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.pipe.NrTelemetryPipe;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.state.TransactionState;
import com.newrelic.opentracing.traces.ErrorTrace;
import com.newrelic.opentracing.util.ProtocolUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class LambdaCollector {

    private static final File NAMED_PIPE_PATH_FILE = new File("/tmp/newrelic-telemetry");
    private static final NrTelemetryPipe NR_TELEMETRY_PIPE = new NrTelemetryPipe(NAMED_PIPE_PATH_FILE);
    private static final String AWS_EXECUTION_ENV = System.getenv("AWS_EXECUTION_ENV");

    private final Queue<LambdaSpanContext> reservoir = new LinkedBlockingQueue<>();

    /**
     * Push finished spans into the reservoir. When the root span finishes, log them only if they're sampled.
     * @param context The span context for the finished span.
     * @param dtState The trace's distributed tracing state.
     * @param txnState The trace's transaction state.
     */
    public void spanFinished(LambdaSpanContext context, DistributedTracingState dtState, TransactionState txnState) {
        reservoir.add(context);

        if (context.getSpan().isRootSpan()) {
            LambdaSpan rootSpan = context.getSpan();

            List<LambdaSpanContext> contexts = new ArrayList<>(reservoir);
            Collections.reverse(contexts);

            Errors errors = new Errors();
            // Record errors after root span has finished. By now, txn name has been set
            contexts.forEach(c -> errors.recordErrors(c, dtState, txnState));
            Object arnTag = rootSpan.getTag("aws.lambda.arn");
            final String arn = arnTag instanceof String ? (String) arnTag : "";

            // Do not collect Spans if sampled=false, clear reservoir and set spans to empty list
            List<LambdaSpan> spans;
            if (!rootSpan.isSampled()) {
                spans = Collections.emptyList();
            } else {
                spans = contexts.stream()
                        .map(LambdaSpanContext::getSpan)
                        .collect(Collectors.toList());
            }

            final TransactionEvent txnEvent = new TransactionEvent(rootSpan, txnState, dtState);
            final List<ErrorEvent> errorEvents = errors.getErrorEvents();
            final List<ErrorTrace> errorTraces = errors.getErrorTraces();
            writeData(arn, spans, txnEvent, errorEvents, errorTraces);
        }
    }

    /**
     * Write all the payload data to the console using standard out. This is the only method that should call the Logger#out method.
     * @param arn the function's ARN
     * @param errorEvents errors captured
     * @param errorTraces error traces captured
     * @param spans spans captured
     * @param txnEvent the transaction event synthesized from the root span
     */
    protected void writeData(String arn,
                           List<LambdaSpan> spans,
                           TransactionEvent txnEvent,
                           List<ErrorEvent> errorEvents,
                           List<ErrorTrace> errorTraces) {
        final Map<String, Object> metadata = ProtocolUtil.getMetadata(arn, AWS_EXECUTION_ENV);
        final Map<String, Object> data = ProtocolUtil.getData(spans, txnEvent, errorEvents, errorTraces);

        final List<Object> payload = Arrays.asList(2, "NR_LAMBDA_MONITORING", metadata, ProtocolUtil.compressAndEncode(JSONObject.toJSONString(data)));

        if (NR_TELEMETRY_PIPE.namedPipeExists()) {
            try {
                NR_TELEMETRY_PIPE.writeToPipe(JSONArray.toJSONString(payload));
                return;
            } catch (IOException ignored) {
                //Errors will fall through, and result in the log write.
            }
        }

        Log.getInstance().out(JSONArray.toJSONString(payload));

        final List<Object> debugPayload = Arrays.asList(2, "DEBUG", metadata, data);
        Log.getInstance().debug(JSONArray.toJSONString(debugPayload));
    }

}
