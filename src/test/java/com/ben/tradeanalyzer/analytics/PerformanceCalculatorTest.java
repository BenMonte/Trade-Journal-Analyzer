package com.ben.tradeanalyzer.analytics;

import com.ben.tradeanalyzer.model.PerformanceSummary;
import com.ben.tradeanalyzer.model.Trade;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceCalculatorTest {

    private final PerformanceCalculator calc = new PerformanceCalculator(10_000);

    // --- Fixtures ---

    private static Trade trade(String date, double pnl, double returnPct, double r) {
        return new Trade(LocalDate.parse(date), pnl, returnPct, r);
    }

    /**
     * 3 winners, 7 losers, realistic low-win-rate trend-following profile
     */
    private static List<Trade> mixedTrades() {
        return List.of(
                trade("2024-01-02", -100, -1.0, -1.0),
                trade("2024-01-03", -80,  -0.8, -0.8),
                trade("2024-01-04", -120, -1.2, -1.2),
                trade("2024-01-05",  500,  5.0,  5.0),
                trade("2024-01-08", -90,  -0.9, -0.9),
                trade("2024-01-09", -110, -1.1, -1.1),
                trade("2024-01-10", -100, -1.0, -1.0),
                trade("2024-01-11",  300,  3.0,  3.0),
                trade("2024-01-12", -95,  -0.95, -0.95),
                trade("2024-01-15",  400,  4.0,  4.0)
        );
    }

    // --- Tests ---

    @Test
    void emptyTradeList() {
        PerformanceSummary s = calc.calculate(List.of());
        assertEquals(0, s.totalTrades());
        assertEquals(0.0, s.winRate());
        assertEquals(0.0, s.maxDrawdownPercent());
    }

    @Test
    void singleWinningTrade() {
        List<Trade> trades = List.of(trade("2024-01-02", 200, 2.0, 2.0));
        PerformanceSummary s = calc.calculate(trades);

        assertEquals(1, s.totalTrades());
        assertEquals(1.0, s.winRate(), 1e-9);
        assertEquals(2.0, s.expectancyR(), 1e-9);
        assertEquals(0.0, s.maxDrawdownPercent(), 1e-9);
        assertEquals(0, s.maxConsecutiveLosses());
    }

    @Test
    void winRate() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // 3 winners out of 10
        assertEquals(0.3, s.winRate(), 1e-9);
    }

    @Test
    void expectancyR() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // winRate=0.3, avgWinR=4.0, lossRate=0.7, avgLossR=-0.9929..
        // expectancy = 0.3 * 4.0 - 0.7 * 0.9929... = 1.2 - 0.695 = 0.505
        assertEquals(0.505, s.expectancyR(), 0.001);
    }

    @Test
    void profitFactor() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // grossWinR = 5.0 + 3.0 + 4.0 = 12.0
        // grossLossR = |(-1.0)+(-0.8)+(-1.2)+(-0.9)+(-1.1)+(-1.0)+(-0.95)| = 6.95
        // profitFactor = 12.0 / 6.95
        assertEquals(12.0 / 6.95, s.profitFactor(), 1e-9);
    }

    @Test
    void profitFactorAllWinners() {
        List<Trade> trades = List.of(
                trade("2024-01-02", 100, 1.0, 1.0),
                trade("2024-01-03", 200, 2.0, 2.0)
        );
        PerformanceSummary s = calc.calculate(trades);
        assertEquals(Double.POSITIVE_INFINITY, s.profitFactor());
    }

    @Test
    void maxConsecutiveLosses() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // Sequence: L L L W L L L W L W → longest streak = 3
        assertEquals(3, s.maxConsecutiveLosses());
    }

    @Test
    void maxDrawdownPercent() {
        // Start at 10000. Equity curve:
        //   9900, 9820, 9700, 10200, 10110, 10000, 9900, 10200, 10105, 10505
        // First peak is initial capital 10000. Drops to 9700 after trade 3.
        // Drawdown = (10000 - 9700) / 10000 = 0.03
        // Later the peak rises to 10200 and drops to 9900 → 300/10200 ≈ 0.02941
        // So worst drawdown is the earlier 0.03.
        PerformanceSummary s = calc.calculate(mixedTrades());
        assertEquals(0.03, s.maxDrawdownPercent(), 1e-9);
    }

    @Test
    void averageR() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // sum of R = -1.0 -0.8 -1.2 +5.0 -0.9 -1.1 -1.0 +3.0 -0.95 +4.0 = 5.05
        // avg = 5.05 / 10 = 0.505
        assertEquals(0.505, s.averageR(), 1e-9);
    }

    @Test
    void stdDevR() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        // Manually: mean = 0.505, sample std dev with ddof=1
        double[] rs = {-1.0, -0.8, -1.2, 5.0, -0.9, -1.1, -1.0, 3.0, -0.95, 4.0};
        double mean = 0.505;
        double sumSq = 0;
        for (double r : rs) {
            sumSq += (r - mean) * (r - mean);
        }
        double expected = Math.sqrt(sumSq / 9);
        assertEquals(expected, s.stdDevR(), 1e-9);
    }

    @Test
    void largestWinAndLoss() {
        PerformanceSummary s = calc.calculate(mixedTrades());
        assertEquals(5.0, s.largestWinR(), 1e-9);
        assertEquals(-1.2, s.largestLossR(), 1e-9);
    }
}
