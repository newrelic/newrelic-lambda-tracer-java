package com.newrelic.opentracing;

import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.traces.ErrorTrace;
import com.newrelic.opentracing.util.ProtocolUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class DataCollection {

    private final AtomicReference<LinkedList<LambdaSpan>> spanReservoir = new AtomicReference<>(new LinkedList<>());
    private final Errors errors = new Errors();
    private final String executionEnv = System.getenv("AWS_EXECUTION_ENV");

    /**
     * Push finished spans into the reservoir. When the root span finishes, log them only if they're sampled.
     */
    void spanFinished(LambdaSpan span) {
        spanReservoir.get().addFirst(span);

        if (span.isRootSpan()) {
            // Record errors after root span has finished. By now, txn name has been set
            for (LambdaSpan lambdaSpan : spanReservoir.get()) {
                errors.recordErrors(lambdaSpan);
            }
            Object arnTag = span.getTag("aws.lambda.arn");
            final String arn = arnTag instanceof String ? (String) arnTag : "";

            final List<LambdaSpan> spans;
            // Do not collect Spans if sampled=false, clear reservoir and set spans to empty list
            if (!span.getPrioritySamplingState().isSampled()) {
                spans = new LinkedList<>();
            } else {
                spans = spanReservoir.get();
            }
            spanReservoir.set(new LinkedList<>());

            final TransactionEvent txnEvent = new TransactionEvent(span);
            final List<ErrorEvent> errorEvents = errors.getAndClearEvents();
            final List<ErrorTrace> errorTraces = errors.getAndClearTraces();
            writeData(arn, executionEnv, spans, txnEvent, errorEvents, errorTraces);
        }
    }

    /**
     * Write all the payload data to the console using standard out. This is the only method that should call the Logger#out method.
     */
    private void writeData(String arn, String executionEnv, List<LambdaSpan> spans, TransactionEvent txnEvent, List<ErrorEvent> errorEvents,
            List<ErrorTrace> errorTraces) {
        final Map<String, Object> metadata = ProtocolUtil.getMetadata(arn, executionEnv);
        final Map<String, Object> data = ProtocolUtil.getData(spans, txnEvent, errorEvents, errorTraces);

        final List<Object> payload = Arrays.asList(2, "NR_LAMBDA_MONITORING", metadata, ProtocolUtil.compressAndEncode(JSONObject.toJSONString(data)));
        Log.getInstance().out(JSONArray.toJSONString(payload));

        final List<Object> debugPayload = Arrays.asList(2, "DEBUG", metadata, data);
        Log.getInstance().debug(JSONArray.toJSONString(debugPayload));
    }

}
