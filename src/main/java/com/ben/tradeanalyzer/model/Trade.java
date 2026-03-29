package com.ben.tradeanalyzer.model;

import java.time.LocalDate;

/**
 * A single completed trade from the journal
 *
 * @param entryDate           date the trade was opened
 * @param tradePnL            realized profit or loss in dollars
 * @param tradeReturnPercent  trade return as a percentage
 * @param rMultiple           gain or loss expressed as a multiple of initial risk
 */
public record Trade(
        LocalDate entryDate,
        double tradePnL,
        double tradeReturnPercent,
        double rMultiple
) {}
