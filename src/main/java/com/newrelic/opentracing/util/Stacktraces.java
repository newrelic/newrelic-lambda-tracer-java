package com.newrelic.opentracing.util;

import java.util.ArrayList;
import java.util.List;

public class Stacktraces {

    private Stacktraces() {
    }

    public static List<String> stackTracesToStrings(StackTraceElement[] stackTraces) {
        if (stackTraces == null || stackTraces.length == 0) {
            return new ArrayList<>();
        }

        final List<String> list = new ArrayList<>();
        for (StackTraceElement e : stackTraces) {
            list.add('\t' + e.toString());
        }

        return list;
    }

}
