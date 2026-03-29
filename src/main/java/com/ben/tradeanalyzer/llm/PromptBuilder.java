package com.ben.tradeanalyzer.llm;

import com.ben.tradeanalyzer.model.PerformanceSummary;

/**
 * Builds the chat prompt sent to the LLM
 * Only receives pre-computed metrics — never raw trade data
 */
public class PromptBuilder {

    public String systemMessage() {
        return "Analyze trading strategy performance metrics. "
                + "Base your analysis only on the metrics provided — do not assume or infer "
                + "information that is not explicitly given. "
                + "Be concise, data-driven, and actionable.";
    }

    public String userMessage(PerformanceSummary s) {
        return """
                The following performance metrics come from a momentum / trend-following equity strategy:

                  Total Trades:          %d
                  Win Rate:              %.2f%%
                  Expectancy (R):        %.2f
                  Average R:             %.2f
                  Avg Win R:             %.2f
                  Avg Loss R:            %.2f
                  Profit Factor:         %.2f
                  Largest Win R:         %.2f
                  Largest Loss R:        %.2f
                  Std Dev R:             %.2f
                  Max Consec. Losses:    %d
                  Max Drawdown:          %.2f%%

                Based only on the metrics above, provide a concise structured analysis covering:
                1. Structural strengths
                2. Structural weaknesses
                3. Risk profile
                4. Long-term consistency
                5. Areas for improvement

                Do not assume any context beyond what is provided.
                Keep the response short — roughly 150 to 250 words, suitable for embedding in a Markdown report.
                Use numbered sections and line breaks for readability.
                """.formatted(
                s.totalTrades(),
                s.winRate() * 100,
                s.expectancyR(),
                s.averageR(),
                s.averageWinR(),
                s.averageLossR(),
                s.profitFactor(),
                s.largestWinR(),
                s.largestLossR(),
                s.stdDevR(),
                s.maxConsecutiveLosses(),
                s.maxDrawdownPercent() * 100
        );
    }
}
