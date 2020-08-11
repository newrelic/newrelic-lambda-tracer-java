package com.newrelic.opentracing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.newrelic.opentracing.pipe.NrTelemetryPipe;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PipeTest {

    File testPayloadPipe = new File("/tmp/newrelic-telemetry-test");

    @BeforeEach
    void createPipe() throws IOException {
        testPayloadPipe.getParentFile().mkdir();
        testPayloadPipe.createNewFile();
    }

    @Test
    void testWrite() throws IOException {
        NrTelemetryPipe nrTelemetryPipe = new NrTelemetryPipe(testPayloadPipe);
        String payload = "I am a lambda payload";
        try {
            nrTelemetryPipe.writeToPipe(payload);
        } catch (IOException e){
        }

        BufferedReader br = new BufferedReader(new FileReader(testPayloadPipe));
        assertEquals(true, nrTelemetryPipe.namedPipeExists());
        assertEquals(true, br.readLine().equals(payload));
    }

    @AfterEach
    void deletePipe() {
        File[] listFiles = testPayloadPipe.getParentFile().listFiles();
        for(File file : listFiles){
            file.delete();
        }
        testPayloadPipe.getParentFile().delete();
    }
}
