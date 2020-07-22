package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newrelic.GlobalTracerTestUtils;
import com.newrelic.TestUtils;
import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.util.ProtocolUtil;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ErrorsTest {

    @BeforeAll
    static void beforeClass() {
        GlobalTracerTestUtils.initTracer(LambdaTracer.INSTANCE);
    }

    @Test
    void testErrors() throws Exception {
        Log.setInstance(new InMemoryLogger());
        final Span span = GlobalTracer.get().buildSpan("span").start();
        try (Scope scope = GlobalTracer.get().activateSpan(span)) {
            try {
                throw new RuntimeException("Ouch! It burns!");
            } catch (Throwable throwable) {
                final Map<String, Object> errorAttributes = new HashMap<>();
                errorAttributes.put("event", Tags.ERROR.getKey());
                errorAttributes.put("error.object", throwable);
                errorAttributes.put("message", throwable.getMessage());
                errorAttributes.put("stack", throwable.getStackTrace());
                errorAttributes.put("error.kind", "Exception");
                span.log(errorAttributes);
            } finally {
                span.finish();
            }
        }

        // first log entry is the encoded version of the payload, the second is unencoded we can manually parse / search
        List<String> logs = Log.getInstance().getLogs();
        assertNotNull(logs);
        assertEquals(2, logs.size());

        String payload = logs.get(0);
        TestUtils.validateFormat(payload);

        String debugPayload = logs.get(1);
        TestUtils.validateDebugFormat(debugPayload);

        // things in 'error_event_data'
        assertTrue(debugPayload.contains("{\"events_seen\":1,\"reservoir_size\":1}"));
        // check the two known that we've seen so far...
        assertTrue(debugPayload.contains("\"error.message\":\"Ouch! It burns!\""));
        assertTrue(debugPayload.contains("\"error.class\":\"java.lang.RuntimeException\""));
        assertTrue(debugPayload.contains("\"error.kind\":\"Exception\""));

        // things in 'error_data'
        assertTrue(debugPayload.contains("\"userAttributes\":{\"error.kind\":\"Exception\"}"));
        assertTrue(debugPayload.contains("\"stack_trace\":[\""));
        assertTrue(debugPayload.contains("\\tcom.newrelic.opentracing.ErrorsTest.testErrors"));
        assertTrue(debugPayload.contains("reflect.NativeMethodAccessorImpl.invoke0(Native Method)"));
    }

    @Test
    void testRequiredAttsErrors() throws ParseException {
        Log.setInstance(new InMemoryLogger());

        final Span span = GlobalTracer.get().buildSpan("span").start();
        try (Scope scope = GlobalTracer.get().activateSpan(span)) {
            try {
                int x = 1 / 0;
            } catch (Throwable throwable) {
                final Map<String, Object> errorAttributes = new HashMap<>();
                errorAttributes.put("event", Tags.ERROR.getKey());
                errorAttributes.put("message", throwable.getMessage());
                errorAttributes.put("stack", throwable.getStackTrace());
                errorAttributes.put("error.kind", "Exception");
                span.log(errorAttributes);
            } finally {
                span.finish();
            }
        }

        // first log entry is the encoded version of the payload, the second is unencoded we can manually parse / search
        List<String> logs = Log.getInstance().getLogs();
        assertNotNull(logs);
        assertEquals(2, logs.size());

        String payload = logs.get(0);
        JSONParser parser = new JSONParser();
        JSONArray object = (JSONArray) parser.parse(payload);
        String dataString = (String) object.get(3);
        assertNotNull(dataString);
        String decodedDataString = ProtocolUtil.decodeAndExtract(dataString);
        Map<String, Object> data = (Map<String, Object>) parser.parse(decodedDataString);
        List<Object> errorEventData = (List<Object>) data.get("error_event_data");
        assertNull(errorEventData);
    }

}
