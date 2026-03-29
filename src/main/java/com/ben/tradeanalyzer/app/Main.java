package com.ben.tradeanalyzer.app;

import com.ben.tradeanalyzer.analytics.PerformanceCalculator;
import com.ben.tradeanalyzer.ingest.ExcelTradeReader;
import com.ben.tradeanalyzer.llm.DiagnosticNarrator;
import com.ben.tradeanalyzer.model.PerformanceSummary;
import com.ben.tradeanalyzer.model.Trade;
import com.ben.tradeanalyzer.reporting.JsonReportWriter;
import com.ben.tradeanalyzer.reporting.MarkdownReportWriter;

import java.nio.file.Path;
import java.util.List;

/**
 * Entry point, parses CLI args, loads trades, computes metrics, and generates reports
 */
public class Main {

    private static final double DEFAULT_CAPITAL = 10_000;
    private static final String DEFAULT_FILE = "TradeDatabase.xlsx";
    private static final Path OUTPUT_DIR = Path.of("output");

    public static void main(String[] args) {
        String filePath = DEFAULT_FILE;
        double capital = DEFAULT_CAPITAL;
        boolean skipLlm = false;

        // CLI arg parsing
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --file requires a value");
                        printUsage();
                        System.exit(1);
                        return;
                    }
                    filePath = args[++i];
                }
                case "--capital" -> {
                    if (i + 1 >= args.length) {
                        System.err.println("Error: --capital requires a value");
                        printUsage();
                        System.exit(1);
                        return;
                    }
                    try {
                        capital = Double.parseDouble(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: --capital must be a number");
                        printUsage();
                        System.exit(1);
                        return;
                    }
                }
                case "--skip-llm" -> skipLlm = true;
                case "--help" -> { printUsage(); return; }
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        System.out.println("Trade Journal Analyzer\n");

        // 1. Ingest
        List<Trade> trades;
        try {
            ExcelTradeReader reader = new ExcelTradeReader();
            trades = reader.read(Path.of(filePath));
            System.out.printf("Loaded %d trades from %s%n%n", trades.size(), filePath);
        } catch (Exception e) {
            System.err.println("Failed to load trades: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 2. Compute metrics
        PerformanceCalculator calculator = new PerformanceCalculator(capital);
        PerformanceSummary summary = calculator.calculate(trades);

        printSummary(summary);

        // 3. LLM diagnostics
        String narrative = null;
        if (!skipLlm) {
            System.out.println("\nGenerating LLM diagnostics...");
            narrative = new DiagnosticNarrator().generate(summary);
            if (narrative != null) {
                System.out.println("\n=== Strategy Diagnostics ===");
                System.out.println(narrative);
            }
        }

        // 4. Write reports
        try {
            Path jsonPath = OUTPUT_DIR.resolve("report.json");
            new JsonReportWriter().write(summary, jsonPath);
            System.out.println("\nJSON report:     " + jsonPath);

            Path mdPath = OUTPUT_DIR.resolve("report.md");
            new MarkdownReportWriter().write(summary, filePath, narrative, mdPath);
            System.out.println("Markdown report: " + mdPath);
        } catch (Exception e) {
            System.err.println("Failed to write reports: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printSummary(PerformanceSummary s) {
        System.out.println("=== Performance Summary ===");
        System.out.printf("  Total Trades:          %d%n", s.totalTrades());
        System.out.printf("  Win Rate:              %.2f%%%n", s.winRate() * 100);
        System.out.printf("  Expectancy (R):        %.2f%n", s.expectancyR());
        System.out.printf("  Average R:             %.2f%n", s.averageR());
        System.out.printf("  Avg Win R:             %.2f%n", s.averageWinR());
        System.out.printf("  Avg Loss R:            %.2f%n", s.averageLossR());
        System.out.printf("  Profit Factor:         %.2f%n", s.profitFactor());
        System.out.printf("  Largest Win R:         %.2f%n", s.largestWinR());
        System.out.printf("  Largest Loss R:        %.2f%n", s.largestLossR());
        System.out.printf("  Std Dev R:             %.2f%n", s.stdDevR());
        System.out.printf("  Max Consec. Losses:    %d%n", s.maxConsecutiveLosses());
        System.out.printf("  Max Drawdown:          %.2f%%%n", s.maxDrawdownPercent() * 100);
    }

    private static void printUsage() {
        System.out.println("""
                Trade Journal Analyzer

                Usage: mvn exec:java -Dexec.args="[options]"

                Options:
                  --file <path>      Path to the Excel trade journal (default: TradeDatabase.xlsx)
                  --capital <amount>  Initial capital in dollars (default: 10000)
                  --skip-llm         Skip LLM narrative even if API key is set
                  --help             Show this help message

                Environment variables:
                  OPENAI_API_KEY     Required for LLM narrative generation
                  OPENAI_MODEL       Override the default model (gpt-4o)
                """);
    }
}
