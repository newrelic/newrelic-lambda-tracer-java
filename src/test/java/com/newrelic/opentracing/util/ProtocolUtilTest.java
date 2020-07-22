package com.newrelic.opentracing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.traces.ErrorTrace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ProtocolUtilTest {

    @Test
    public void getMetadata() {
        final Map<String, Object> metadata = ProtocolUtil.getMetadata("myARN", "executionEnvironment");
        assertEquals(16, metadata.get("protocol_version"));
        assertEquals("java", metadata.get("agent_language"));
        assertEquals("executionEnvironment", metadata.get("execution_environment"));
        assertEquals("myARN", metadata.get("arn"));
        assertEquals(2, metadata.get("metadata_version"));
        assertNotNull(metadata.get("agent_version"));
    }

    @Test
    public void getData() {
        List<LambdaSpan> spans = createTestSpans(2);
        TransactionEvent txnEvent = new TransactionEvent(spans.get(0));
        List<ErrorEvent> errorEvents = new ArrayList<>();
        List<ErrorTrace> trace = new ArrayList<>();

        Map<String, Object> data = ProtocolUtil.getData(spans, txnEvent, errorEvents, trace);
        final List txnEvents = (List) data.get("analytic_event_data");
        final Map txnInfo = (Map) txnEvents.get(1);
        assertEquals(1, txnInfo.get("events_seen"));
        assertEquals(1, txnInfo.get("reservoir_size"));

        final List txns = (List) txnEvents.get(2);
        final TransactionEvent txn = (TransactionEvent) txns.get(0);
        assertNotNull(txn);

        final List spanEventData = (List) data.get("span_event_data");
        final Map spansInfo = (Map) spanEventData.get(1);
        assertEquals(2, spansInfo.get("events_seen"));
        assertEquals(2, spansInfo.get("reservoir_size"));
        final List spanEvents = (List) spanEventData.get(2);
        assertEquals(2, spanEvents.size());

        final LambdaSpan spanEvent = (LambdaSpan) spanEvents.get(0);
        assertNotNull(spanEvent);

        assertNull(data.get("error_event_data"));
        assertNull(data.get("error_data"));
    }

    private List<LambdaSpan> createTestSpans(int numberOfSpans) {
        final List<LambdaSpan> spans = new ArrayList<>();
        final Map<String, Object> tags = new HashMap<>();
        tags.put("someTag", "value");
        tags.put("secondTag", "secondValue");

        LambdaSpan parentSpan = null;
        while (numberOfSpans-- > 0) {
            final LambdaSpan lambdaSpan = new LambdaSpan("operationName", 1234L, 1234L,
                    tags, parentSpan, "guid", "txnId");
            parentSpan = lambdaSpan;
            spans.add(lambdaSpan);
        }
        return spans;
    }

}