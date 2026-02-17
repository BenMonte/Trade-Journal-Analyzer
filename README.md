# Trade Journal Analyzer

A Java CLI tool that reads an Excel trade journal, computes deterministic
performance metrics (win rate, expectancy, profit factor, max drawdown, and
more), and generates structured JSON and Markdown reports.

An optional LLM integration can produce a short narrative summary of the
computed metrics. The LLM does not compute or alter any numbers — all analytics
are performed in Java.

## Features

- **Excel ingestion** — Reads `.xlsx` trade journals via Apache POI, handles
  formula cells and malformed rows gracefully
- **Deterministic analytics** — Computes 12 performance metrics entirely in
  Java: win rate, expectancy, profit factor, R-multiple statistics, max
  drawdown, and more
- **Dual-format reporting** — Outputs both JSON (machine-readable) and Markdown
  (human-readable) to `output/`
- **Optional LLM narrative** — Sends pre-computed metrics to OpenAI for a
  written summary; fully skippable via `--skip-llm`
- **Unit tested** — Core analytics layer covered by JUnit 5 tests with
  deterministic fixtures

## Architecture

```
com.ben.tradeanalyzer
├── app/
│   └── Main.java                   # CLI entry point and argument parsing
├── model/
│   ├── Trade.java                  # Trade record (date, PnL, return%, R-multiple)
│   └── PerformanceSummary.java     # Immutable 12-field metrics record
├── ingest/
│   └── ExcelTradeReader.java       # .xlsx → List<Trade>
├── analytics/
│   └── PerformanceCalculator.java  # List<Trade> → PerformanceSummary
├── reporting/
│   ├── JsonReportWriter.java       # Summary → JSON file
│   └── MarkdownReportWriter.java   # Summary → Markdown file
└── llm/
    ├── PromptBuilder.java          # Builds system + user messages from metrics
    ├── LlmClient.java              # OpenAI chat completions client (java.net.http)
    └── DiagnosticNarrator.java     # Orchestrates LLM call, handles failures
```

Built with **Maven**. Requires **Java 17+**.

## Prerequisites

- Java 17 or later
- Maven 3.8+
- An `.xlsx` trade journal (see [Input Format](#input-format))
- *(Optional)* `OPENAI_API_KEY` environment variable for LLM narrative

## Build and Test

```bash
mvn compile        # compile only
mvn test           # compile + run unit tests
```

## Usage

```bash
# Default: reads TradeDatabase.xlsx, $10k capital, LLM enabled if API key is set
mvn exec:java

# Skip LLM narrative
mvn exec:java -Dexec.args="--skip-llm"

# Custom file and capital
mvn exec:java -Dexec.args="--file MyTrades.xlsx --capital 25000"

# Show help
mvn exec:java -Dexec.args="--help"
```

### CLI Options

| Option | Default | Description |
|---|---|---|
| `--file <path>` | `TradeDatabase.xlsx` | Path to the Excel trade journal |
| `--capital <amount>` | `10000` | Initial capital in dollars |
| `--skip-llm` | off | Skip LLM narrative even if `OPENAI_API_KEY` is set |
| `--help` | — | Show usage information |

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `OPENAI_API_KEY` | No | Enables LLM narrative generation |
| `OPENAI_MODEL` | No | Override the default model (`gpt-4o`) |

## Input Format

The tool expects an `.xlsx` file with at least these four columns:

| Column | Example | Description |
|---|---|---|
| `Entry Date` | `2024-03-15` | Date the trade was opened |
| `Trade P&L ($)` | `342.50` | Realized profit or loss in dollars |
| `Trade Return (%)` | `3.42` | Trade return as a percentage |
| `R-Multiple` | `1.52 R` | Gain/loss as a multiple of initial risk |

Rows with missing or unparseable values are skipped with a warning.

## Output

The tool writes two files to the `output/` directory:

- **`report.json`** — Full-precision metrics in JSON
- **`report.md`** — Formatted Markdown report with metrics table and optional
  LLM narrative section

### Sample Console Output

```
Trade Journal Analyzer

Loaded 830 trades from TradeDatabase.xlsx

=== Performance Summary ===
  Total Trades:          830
  Win Rate:              25.06%
  Expectancy (R):        0.31
  Average R:             0.31
  Avg Win R:             4.00
  Avg Loss R:            -0.92
  Profit Factor:         1.45
  Largest Win R:         25.76
  Largest Loss R:        -4.24
  Std Dev R:             3.11
  Max Consec. Losses:    18
  Max Drawdown:          37.00%

JSON report:     output/report.json
Markdown report: output/report.md
```

## Computed Metrics

All metrics are computed deterministically in `PerformanceCalculator`:

| Metric | Description |
|---|---|
| Total Trades | Number of valid trade rows |
| Win Rate | Fraction of trades with positive P&L |
| Expectancy (R) | winRate × avgWinR − lossRate × \|avgLossR\| |
| Average Win / Loss R | Mean R-multiple of winning / losing trades |
| Profit Factor | Gross winning R ÷ gross losing R |
| Largest Win / Loss R | Best and worst single-trade R-multiples |
| Std Dev R | Sample standard deviation of R-multiples |
| Max Consecutive Losses | Longest streak of losing trades |
| Max Drawdown % | Largest peak-to-trough equity decline |

