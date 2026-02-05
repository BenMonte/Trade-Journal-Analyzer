package com.ben.tradeanalyzer.reporting;

import com.ben.tradeanalyzer.model.PerformanceSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the performance summary to a pretty-printed JSON file.
 */
public class JsonReportWriter {

    private final ObjectMapper mapper;

    public JsonReportWriter() {
        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(PerformanceSummary summary, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        mapper.writeValue(outputFile.toFile(), summary);
    }
}
