package com.ben.tradeanalyzer.analytics;

import com.ben.tradeanalyzer.model.PerformanceSummary;
import com.ben.tradeanalyzer.model.Trade;

import java.util.List;

/**
 * Computes performance metrics (win rate, drawdown, etc.) from a list of trades
 */
public class PerformanceCalculator {

    private final double initialCapital;

    public PerformanceCalculator(double initialCapital) {
        this.initialCapital = initialCapital;
    }

    public PerformanceSummary calculate(List<Trade> trades) {
        if (trades.isEmpty()) {
            return new PerformanceSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int total = trades.size();

        List<Trade> winners = trades.stream().filter(t -> t.tradePnL() > 0).toList();
        List<Trade> losers = trades.stream().filter(t -> t.tradePnL() <= 0).toList();

        double winRate = (double) winners.size() / total;

        double averageR = trades.stream()
                .mapToDouble(Trade::rMultiple)
                .average()
                .orElse(0.0);

        double averageWinR = winners.stream()
                .mapToDouble(Trade::rMultiple)
                .average()
                .orElse(0.0);

        double averageLossR = losers.stream()
                .mapToDouble(Trade::rMultiple)
                .average()
                .orElse(0.0);

        // Expectancy = winRate * avgWinR - lossRate * |avgLossR|
        double lossRate = (double) losers.size() / total;
        double expectancyR = winRate * averageWinR - lossRate * Math.abs(averageLossR);

        double grossWinR = winners.stream().mapToDouble(Trade::rMultiple).sum();
        double grossLossR = losers.stream().mapToDouble(t -> Math.abs(t.rMultiple())).sum();
        double profitFactor = grossLossR == 0
                ? (grossWinR > 0 ? Double.POSITIVE_INFINITY : 0.0)
                : grossWinR / grossLossR;

        double largestWinR = trades.stream().mapToDouble(Trade::rMultiple).max().orElse(0.0);
        double largestLossR = trades.stream().mapToDouble(Trade::rMultiple).min().orElse(0.0);

        double stdDevR = sampleStdDev(trades);
        int maxConsecutiveLosses = maxConsecLosses(trades);
        double maxDrawdownPercent = maxDrawdown(trades);

        return new PerformanceSummary(
                total, winRate, expectancyR, averageR,
                averageWinR, averageLossR, profitFactor,
                largestWinR, largestLossR, stdDevR,
                maxConsecutiveLosses, maxDrawdownPercent
        );
    }

    private double sampleStdDev(List<Trade> trades) {
        if (trades.size() < 2) return 0.0;

        double mean = trades.stream().mapToDouble(Trade::rMultiple).average().orElse(0.0);
        double sumSqDiff = trades.stream()
                .mapToDouble(t -> {
                    double diff = t.rMultiple() - mean;
                    return diff * diff;
                })
                .sum();

        return Math.sqrt(sumSqDiff / (trades.size() - 1));
    }

    private int maxConsecLosses(List<Trade> trades) {
        int max = 0;
        int current = 0;
        for (Trade t : trades) {
            if (t.tradePnL() <= 0) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private double maxDrawdown(List<Trade> trades) {
        double peak = initialCapital;
        double equity = initialCapital;
        double worstDrawdown = 0.0;

        for (Trade t : trades) {
            equity += t.tradePnL();
            if (equity > peak) {
                peak = equity;
            }
            double drawdown = (peak - equity) / peak;
            if (drawdown > worstDrawdown) {
                worstDrawdown = drawdown;
            }
        }
        return worstDrawdown;
    }
}
