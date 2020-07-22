package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.util.TimeUtil;
import io.opentracing.tag.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LambdaSpanTest {

  private ByteArrayOutputStream outContent;

  @BeforeEach
  void setupStreams() {
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
  }

  @AfterEach
  void resetStreams() {
    outContent.reset();
  }

  @Test
  void durationTest() {
    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
        .createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid",
            "txnId");
    sleep(80);
    span.finish();

    final long end = System.currentTimeMillis();
    final long maxExpectedDurationInMs = end - start;

    final float spanDurationInMs = TimeUnit.MICROSECONDS.toMillis(span.getDurationInMicros());
    assertTrue(spanDurationInMs <= maxExpectedDurationInMs, "Incorrect span duration: " + spanDurationInMs);

    final float durationInSec = (float) span.getIntrinsics().get("duration");
    assertTrue(durationInSec <= (maxExpectedDurationInMs / TimeUtil.MILLISECONDS_PER_SECOND),
        "Incorrect span duration in sec: " + durationInSec);
  }

  @Test
  void noErrorTag() {
    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
        .createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid",
            "txnId");
    sleep(80);
    span.setTag("error", true);
    span.finish();
    assertFalse(span.getUserAttributes().containsKey("error"));
  }

  @Test
  void spanCategory() {
    final long start = System.currentTimeMillis();

    final LambdaSpan span = SpanTestUtils
        .createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid",
            "txnId");
    sleep(80);
    span.setTag("http.status_code", "404");
    span.setTag("span.kind", "client");
    span.finish();

    final Map<String, Object> intrinsics = span.getIntrinsics();
    assertEquals("http", intrinsics.get("category"));
  }

  @Test
  void spanParenting() {
    final LambdaSpan parent = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
        System.nanoTime(), new HashMap<>(), null, "parentGuid", "txnId");

    final LambdaSpan child = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
        System.nanoTime(), new HashMap<>(), parent, "childGuid", "txnId");

    final LambdaSpan grandChild = SpanTestUtils
        .createSpan("operationName", System.currentTimeMillis(),
            System.nanoTime(), new HashMap<>(), child, "grandChildGuid", "txnId");

    final LambdaSpan greatGrandChild = SpanTestUtils
        .createSpan("operationName", System.currentTimeMillis(),
            System.nanoTime(), new HashMap<>(), grandChild, "greatGrandChild", "txnId");

    assertNull(parent.getIntrinsics().get("parentId"));
    assertEquals("parentGuid", child.getIntrinsics().get("parentId"));
    assertEquals("childGuid", grandChild.getIntrinsics().get("parentId"));
    assertEquals("grandChildGuid", greatGrandChild.getIntrinsics().get("parentId"));
  }

  @Test
  void testSampledTrue() {
    Log.setInstance(new InMemoryLogger());

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
        .createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid",
            "txnId");
    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);
    span.finish();

    List<String> logs = Log.getInstance().getLogs();
    final String debugLogEntry = logs.get(1);
    // Should be one Span collected and logged. JSON should contain both the "span_event_data" and "analytic_event_data" hashes.
    assertTrue(containsEventJsonKey(debugLogEntry, "span_event_data"));
    assertTrue(containsEventJsonKey(debugLogEntry, "analytic_event_data"));
  }

  @Test
  void testSampledFalse() {
    Log.setInstance(new InMemoryLogger());

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
        .createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid",
            "txnId");
    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(false);
    span.finish();

    List<String> logs = Log.getInstance().getLogs();
    final String debugLogEntry = logs.get(1);
    // Should be no Spans collected and logged. JSON should contain the "analytic_event_data" hash but not the "span_event_data" hash.
    assertFalse(containsEventJsonKey(debugLogEntry, "span_event_data"));
    assertTrue(containsEventJsonKey(debugLogEntry, "analytic_event_data"));
  }

  @Test
  void testNullTagsMap() {
    Log.setInstance(new InMemoryLogger());

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
            .createSpan("operationName", start, System.nanoTime(), null, null, "guid",
                    "txnId");

    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);

    // No tags added and null map doesn't cause NPE
    assertTrue(span.getTags().isEmpty());
    assertTrue(span.getUserAttributes().isEmpty());

    span.finish();
  }

  @Test
  void testValidTagsMap() {
    Log.setInstance(new InMemoryLogger());

    final HashMap<String, Object> input = new HashMap<>();
    input.put("key1", "value");
    input.put("key2", true);
    input.put("key3", 2.0f);

    final HashMap<String, Object> expected = new HashMap<>();
    expected.put("key1", "value");
    expected.put("key2", true);
    expected.put("key3", 2.0f);

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
            .createSpan("operationName", start, System.nanoTime(), input, null, "guid",
                    "txnId");

    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);

    // Valid tags added
    assertEquals(expected, span.getTags());
    assertEquals(expected, span.getUserAttributes());

    span.finish();
  }

  @Test
  void testNullValuesTagsMap() {
    Log.setInstance(new InMemoryLogger());

    final HashMap<String, Object> input = new HashMap<>();
    input.put("key1", "value");
    input.put("key2", true);
    input.put("key3", 2.0f);
    input.put(null, 2.0f);
    input.put("key5", null);
    input.put(null, null);

    final HashMap<String, Object> expected = new HashMap<>();
    expected.put("key1", "value");
    expected.put("key2", true);
    expected.put("key3", 2.0f);

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
            .createSpan("operationName", start, System.nanoTime(), input, null, "guid",
                    "txnId");

    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);

    // Valid tags added, null keys/values omitted
    assertEquals(expected, span.getTags());
    assertEquals(expected, span.getUserAttributes());

    span.finish();
  }

  @Test
  void testValidSetTagValues() {
    Log.setInstance(new InMemoryLogger());

    final HashMap<String, Object> expected = new HashMap<>();
    expected.put("key1", "value");
    expected.put("key2", true);
    expected.put("key3", 2.0f);
    expected.put("key4", true);
    expected.put("key5", 1);
    expected.put("key6", "value");
    expected.put("key7", 0);

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
            .createSpan("operationName", start, System.nanoTime(), null, null, "guid",
                    "txnId");
    span.setTag("key1", "value");
    span.setTag("key2", true);
    span.setTag("key3", 2.0f);
    span.setTag(new BooleanTag("key4"), true);
    span.setTag(new IntTag("key5"), 1);
    span.setTag(new StringTag("key6"), "value");
    span.setTag(new IntOrStringTag("key7"), 0);

    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);

    // Valid tags added
    assertEquals(expected, span.getTags());
    assertEquals(expected, span.getUserAttributes());

    span.finish();
  }

  @Test
  void testNullSetTagValues() {
    Log.setInstance(new InMemoryLogger());

    final long start = System.currentTimeMillis();
    final LambdaSpan span = SpanTestUtils
            .createSpan("operationName", start, System.nanoTime(), null, null, "guid",
                    "txnId");
    span.setTag(null, false);
    span.setTag((String) null, "value");
    span.setTag("key5", (Number) null);

    sleep(80);
    span.getPrioritySamplingState().setSampledAndGeneratePriority(true);

    // No tags added and null keys/values don't cause NPE
    assertTrue(span.getTags().isEmpty());
    assertTrue(span.getUserAttributes().isEmpty());

    span.finish();
  }

  private boolean containsEventJsonKey(String jsonString, String key) {
    JSONParser parser = new JSONParser();
    try {
      // JSONArray with the 4th entry being a JSONObject representing event data
      JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
      final JSONObject eventHash = (JSONObject) jsonArray.get(3);
      return eventHash.containsKey(key);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return false;
  }

  private void sleep(int sleepInMs) {
    final long end = System.currentTimeMillis() + sleepInMs;
    while (System.currentTimeMillis() <= end) {
    }
  }

}