package com.ben.tradeanalyzer.reporting;

import com.ben.tradeanalyzer.model.PerformanceSummary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes the performance summary and optional LLM narrative to a Markdown report
 */
public class MarkdownReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void write(PerformanceSummary summary, String inputFileName,
                      String llmNarrative, Path outputFile) throws IOException {
        String md = buildMarkdown(summary, inputFileName, llmNarrative);
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, md);
    }

    private String buildMarkdown(PerformanceSummary s, String inputFileName,
                                 String llmNarrative) {
        StringBuilder md = new StringBuilder();

        md.append("# Trade Journal Analyzer — Performance Report\n\n");
        md.append("**Generated:** ").append(LocalDateTime.now().format(TIMESTAMP_FMT)).append("\n\n");
        md.append("**Input file:** ").append(inputFileName).append("\n\n");

        // --- Deterministic metrics section ---
        md.append("## Performance Metrics\n\n");
        md.append("| Metric | Value |\n");
        md.append("|---|---|\n");
        md.append(row("Total Trades", String.valueOf(s.totalTrades())));
        md.append(row("Win Rate", String.format("%.2f%%", s.winRate() * 100)));
        md.append(row("Expectancy (R)", String.format("%.2f", s.expectancyR())));
        md.append(row("Average R", String.format("%.2f", s.averageR())));
        md.append(row("Avg Win R", String.format("%.2f", s.averageWinR())));
        md.append(row("Avg Loss R", String.format("%.2f", s.averageLossR())));
        md.append(row("Profit Factor", String.format("%.2f", s.profitFactor())));
        md.append(row("Largest Win R", String.format("%.2f", s.largestWinR())));
        md.append(row("Largest Loss R", String.format("%.2f", s.largestLossR())));
        md.append(row("Std Dev R", String.format("%.2f", s.stdDevR())));
        md.append(row("Max Consecutive Losses", String.valueOf(s.maxConsecutiveLosses())));
        md.append(row("Max Drawdown", String.format("%.2f%%", s.maxDrawdownPercent() * 100)));
        md.append("\n");

        // --- LLM narrative section ---
        md.append("## Strategy Diagnostics (LLM)\n\n");
        if (llmNarrative != null && !llmNarrative.isBlank()) {
            md.append(llmNarrative).append("\n");
        } else {
            md.append("*LLM diagnostics were not generated for this report.*\n");
        }

        return md.toString();
    }

    private static String row(String label, String value) {
        return "| " + label + " | " + value + " |\n";
    }
}
