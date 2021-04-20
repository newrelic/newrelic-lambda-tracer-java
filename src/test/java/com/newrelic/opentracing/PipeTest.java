package com.newrelic.opentracing;

import com.newrelic.opentracing.pipe.NrTelemetryPipe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipeTest {

    private final File testPayloadPipe = new File("/tmp/newrelic-telemetry-test");
    private boolean shouldDeleteParent = false;

    @BeforeEach
    void createPipe() throws IOException {
        shouldDeleteParent = testPayloadPipe.getParentFile().mkdir();
        testPayloadPipe.createNewFile();
    }

    @Test
    void testWrite() throws IOException {
        NrTelemetryPipe nrTelemetryPipe = new NrTelemetryPipe(testPayloadPipe);
        String payload = "I am a lambda payload";
        try {
            nrTelemetryPipe.writeToPipe(payload);
        } catch (IOException e) {
            fail(e);
        }

        BufferedReader br = new BufferedReader(new FileReader(testPayloadPipe));
        assertTrue(nrTelemetryPipe.namedPipeExists());
        assertEquals(br.readLine(), payload);
    }

    @AfterEach
    void deletePipe() {
        assertTrue(testPayloadPipe.delete());
        if (shouldDeleteParent) {
            final File parentFile = testPayloadPipe.getParentFile();
            File[] listFiles = parentFile.listFiles();
            for (File file : listFiles) {
                file.delete();
            }
            parentFile.delete();
        }
    }
}
