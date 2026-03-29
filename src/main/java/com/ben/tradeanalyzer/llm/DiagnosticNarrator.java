package com.ben.tradeanalyzer.llm;

import com.ben.tradeanalyzer.model.PerformanceSummary;

/**
 * Orchestrates the LLM flow, builds the prompt, calls the API, and handles failures
 */
public class DiagnosticNarrator {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    public String generate(PerformanceSummary summary) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[info] OPENAI_API_KEY not set — skipping LLM diagnostics.");
            return null;
        }

        try {
            LlmClient client = new LlmClient(apiKey);
            String systemMsg = promptBuilder.systemMessage();
            String userMsg = promptBuilder.userMessage(summary);
            return client.chatCompletion(systemMsg, userMsg);
        } catch (Exception e) {
            System.err.println("[warn] LLM diagnostics failed: " + e.getMessage());
            return null;
        }
    }
}
