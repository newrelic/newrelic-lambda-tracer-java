/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DistributedTraceUtil {

    /**
     * A thread local which holds a {@link Random} whose seed is the thread id.
     */
    public static final ThreadLocalRandom random = ThreadLocalRandom.current();

    // Payload constants
    public static final String VERSION = "v";
    public static final String DATA = "d";
    public static final String PARENT_TYPE = "ty";
    public static final String APP_PARENT_TYPE = "App";
    public static final String ACCOUNT_ID = "ac";
    public static final String TRUSTED_ACCOUNT_KEY = "tk";
    public static final String APPLICATION_ID = "ap";
    public static final String TIMESTAMP = "ti";
    public static final String GUID = "id";
    public static final String TRACE_ID = "tr";
    public static final String TX = "tx";
    public static final String PRIORITY = "pr";
    public static final String SAMPLED = "sa";

    // Instantiate a new DecimalFormat instance as it is not thread safe
    private static final ThreadLocal<DecimalFormat> FORMATTER =
            ThreadLocal.withInitial(() -> {
                DecimalFormat format = new DecimalFormat();
                DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
                if (symbols.getDecimalSeparator() == ',') {
                    format.applyLocalizedPattern("#,######");
                } else {
                    format.applyPattern("#.######");
                }
                return format;
            });

    private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private DistributedTraceUtil() {
    }

    // Note that the digits are generated in "reverse order", which is perfectly fine here.
    public static String generateGuid() {
        long random = DistributedTraceUtil.random.nextLong();
        char[] result = new char[16];
        for (int i = 0; i < 16; ++i) {
            result[i] = hexchars[(int) (random & 0xF)];
            random >>= 4;
        }
        return new String(result);
    }

    public static boolean isSampledPriority(float priority) {
        return priority >= 1.0f;
    }

    public static float nextTruncatedFloat() {
        float next = 0.0f;
        try {
            next = Float.parseFloat(FORMATTER.get().format(DistributedTraceUtil.random.nextFloat()).replace(',', '.'));
        } catch (NumberFormatException e) {
        }
        return next;
    }

}
