/*
 * Copyright 2020 New Relic Corporation. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.newrelic.opentracing.logging;

/**
 * To use the logger, call Log.getInstance() and then use whatever methods are on the Logger interface.
 */
public class Log {

    /**
     * Defaults to a console logger, which only logs payload data to the console, unless a special environment
     * variable has been set, which will instead use a debug logger to also log debug messages to the console.
     */
    private static class InstanceHolder {
        public static Logger instance;

        static {
            String debug = System.getenv("NEW_RELIC_DEBUG");
            if (debug != null && debug.equalsIgnoreCase("true")) {
                instance = new DebugLogger();
            } else {
                instance = new ConsoleLogger();
            }
        }
    }

    /**
     * Get the singleton instance.
     *
     * @return Instance of the Logger
     */
    public static Logger getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * This setter can be used for testing to use a custom implementation, or an in-memory logger for example.
     *
     * @param logger Instance of the Logger
     */
    public static void setInstance(Logger logger) {
        InstanceHolder.instance = logger;
    }

    private Log() {
    }

}
