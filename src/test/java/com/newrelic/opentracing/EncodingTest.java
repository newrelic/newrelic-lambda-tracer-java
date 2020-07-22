package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newrelic.opentracing.util.ProtocolUtil;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class EncodingTest {

    @Test
    public void compressAndEncode() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("aws.requestId", "4b4a1f04-8ff1-4143-b1ac-a2d5de29dd4e");
        tags.put("aws.lambda.arn", "arn:aws:lambda:us-east-1:383735328703:function:handleRequest");
        tags.put("aws.lambda.eventSource.arn", "");
        tags.put("aws.lambda.coldStart", true);

        final LambdaSpan parent = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), tags, null, "parentGuid", "txnId");

        final LambdaSpan child = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), parent, "childGuid", "txnId");

        final LambdaSpan grandChild = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), child, "grandChildGuid", "txnId");

        final LambdaSpan greatGrandChild = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), grandChild, "greatGrandChild", "txnId");

        validate(parent);
        validate(child);
        validate(grandChild);
        validate(greatGrandChild);
    }

    private void validate(LambdaSpan span) {
        String original = span.toString();
        String encoded = ProtocolUtil.compressAndEncode(original);
        String decoded = ProtocolUtil.decodeAndExtract(encoded);
        assertEquals(original, decoded);
    }

}
