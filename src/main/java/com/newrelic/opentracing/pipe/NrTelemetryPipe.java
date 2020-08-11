package com.newrelic.opentracing.pipe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class NrTelemetryPipe {
    private final File namedPipePath;

    public NrTelemetryPipe(File namedPipePath) {
        this.namedPipePath = namedPipePath;
    }

    public boolean namedPipeExists() {
        return namedPipePath.exists();
    }

    public void writeToPipe(String payload) throws IOException {
        try (BufferedWriter pipe = new BufferedWriter(new FileWriter(namedPipePath, false))){
            pipe.write(payload);
        }
    }
}
