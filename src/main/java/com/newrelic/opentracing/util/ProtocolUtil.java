/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.util;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.Event;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.traces.ErrorTrace;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ProtocolUtil {
    private static final String VERSION;

    static {
        Package thisPackage = ProtocolUtil.class.getPackage();
        VERSION = Optional.ofNullable(thisPackage.getImplementationVersion())
                .orElse("Unknown Version");
    }

    private ProtocolUtil() {
    }

    /**
     * Metadata is not compressed or encoded.
     *
     * @param arn AWS Resource Name
     * @param executionEnv AWS execution environment
     * @return Map of metadata
     */
    public static Map<String, Object> getMetadata(String arn, String executionEnv) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("protocol_version", 16);
        metadata.put("arn", arn);
        metadata.put("execution_environment", executionEnv);
        metadata.put("agent_version", VERSION);
        metadata.put("metadata_version", 2);
        metadata.put("agent_language", "java");
        return metadata;
    }

    public static Map<String, Object> getData(List<LambdaSpan> spans, TransactionEvent transactionEvent, List<ErrorEvent> errorEvents,
                                              List<ErrorTrace> errorTraces) {
        Map<String, Object> data = new HashMap<>();

        if (spans.size() > 0) {
            addEvents(spans, data, "span_event_data");
        }
        if (transactionEvent != null) {
            addEvents(Collections.singletonList(transactionEvent), data, "analytic_event_data");
        }
        if (errorEvents.size() > 0) {
            addEvents(errorEvents, data, "error_event_data");
        }
        if (errorTraces.size() > 0) {
            data.put("error_data", Arrays.asList(null, errorTraces));
        }

        return data;
    }

    private static void addEvents(List<? extends Event> events, Map<String, Object> data, String eventKey) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        final Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("events_seen", events.size());
        eventInfo.put("reservoir_size", events.size());
        list.add(1, eventInfo);
        list.add(2, events);
        data.put(eventKey, list);
    }

    /**
     * gzip compress and base64 encode.
     *
     * @param source String to be compressed and encoded
     * @return String compressed and encoded
     */
    public static String compressAndEncode(String source) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(output);
            gzip.write(source.getBytes(UTF_8));
            gzip.flush();
            gzip.close();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ignored) {
        }
        return "";
    }

    /**
     * base64 decode and gzip extract.
     *
     * @param source String to be decoded and extracted
     * @return String decoded and extracted
     */
    public static String decodeAndExtract(String source) {
        try {
            byte[] bytes = Base64.getDecoder().decode(source);
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            GZIPInputStream gzip = new GZIPInputStream(input);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzip, UTF_8));

            String line;
            StringBuilder outStr = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }

            return outStr.toString();
        } catch (IOException ignored) {
        }
        return "";
    }

}
