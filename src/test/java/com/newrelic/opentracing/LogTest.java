package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newrelic.opentracing.logging.ConsoleLogger;
import com.newrelic.opentracing.logging.DebugLogger;
import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogTest {

    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setupStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Only 'out' should have any logs.
     */
    @Test
    void testConsoleLogger() {
        Log.setInstance(new ConsoleLogger());

        Log.getInstance().out("test");
        assertEquals("test\n", outContent.toString());
        outContent.reset();

        Log.getInstance().debug("debug");
        assertEquals("", outContent.toString());
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        assertNotNull(logs);
        assertEquals(0, logs.size());
    }

    /**
     * Both 'out' and 'debug' should have logs.
     */
    @Test
    public void testDebugLogger() throws IOException {
        Log.setInstance(new DebugLogger());

        Log.getInstance().out("test");
        assertEquals("test\n", outContent.toString());
        outContent.reset();

        Log.getInstance().debug("debug");
        assertEquals("nr_debug: debug\n", outContent.toString());
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        assertNotNull(logs);
        assertEquals(0, logs.size());
    }

    /**
     * Neither 'out' nor 'debug' should have logs.
     */
    @Test
    public void testInMemoryLogger() throws IOException {
        Log.setInstance(new InMemoryLogger());

        Log.getInstance().out("test");
        assertEquals("", outContent.toString());
        outContent.reset();

        Log.getInstance().debug("debug");
        assertEquals("", outContent.toString());
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        assertNotNull(logs);
        assertEquals(2, logs.size());
        assertEquals("test", logs.get(0));
        assertEquals("debug", logs.get(1));
    }

}
