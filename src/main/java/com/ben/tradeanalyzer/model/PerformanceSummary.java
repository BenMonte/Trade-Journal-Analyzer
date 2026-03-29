package com.ben.tradeanalyzer.model;

/**
 * Immutable container for all computed performance metrics
 *
 * @param totalTrades          number of completed trades
 * @param winRate              fraction of trades with positive P&L (0.0–1.0)
 * @param expectancyR          expected R per trade
 * @param averageR             mean R-multiple across all trades
 * @param averageWinR          mean R-multiple of winning trades
 * @param averageLossR         mean R-multiple of losing trades (negative)
 * @param profitFactor         gross winning R / gross losing R
 * @param largestWinR          highest single-trade R-multiple
 * @param largestLossR         lowest single-trade R-multiple (most negative)
 * @param stdDevR              standard deviation of R-multiples
 * @param maxConsecutiveLosses longest streak of consecutive losing trades
 * @param maxDrawdownPercent   largest peak-to-trough equity decline (0.0–1.0)
 */
public record PerformanceSummary(
        int totalTrades,
        double winRate,
        double expectancyR,
        double averageR,
        double averageWinR,
        double averageLossR,
        double profitFactor,
        double largestWinR,
        double largestLossR,
        double stdDevR,
        int maxConsecutiveLosses,
        double maxDrawdownPercent
) {}
