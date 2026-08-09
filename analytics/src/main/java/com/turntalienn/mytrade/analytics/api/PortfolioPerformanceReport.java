package com.turntalienn.mytrade.analytics.api;

import java.math.BigDecimal;

/**
 * Immutable performance and lower-tail risk results for one return series.
 * Drawdown, VaR, and expected shortfall are reported as non-negative loss
 * magnitudes.
 */
public record PortfolioPerformanceReport(
        int observations,
        BigDecimal totalReturn,
        BigDecimal annualizedReturn,
        BigDecimal realAnnualizedReturn,
        BigDecimal annualizedVolatility,
        RiskAdjustedRatio sharpeRatio,
        RiskAdjustedRatio sortinoRatio,
        BigDecimal maximumDrawdown,
        BigDecimal historicalValueAtRisk,
        BigDecimal expectedShortfall
) {

    public PortfolioPerformanceReport {
        if (observations < 2) {
            throw new IllegalArgumentException("observations must be at least two");
        }
        requireMetric("totalReturn", totalReturn);
        requireMetric("annualizedReturn", annualizedReturn);
        requireMetric("realAnnualizedReturn", realAnnualizedReturn);
        requireMetric("annualizedVolatility", annualizedVolatility);
        if (annualizedVolatility.signum() < 0) {
            throw new IllegalArgumentException("annualizedVolatility must not be negative");
        }
        requireRatio("sharpeRatio", sharpeRatio);
        requireRatio("sortinoRatio", sortinoRatio);
        requireLossMetric("maximumDrawdown", maximumDrawdown);
        requireLossMetric("historicalValueAtRisk", historicalValueAtRisk);
        requireLossMetric("expectedShortfall", expectedShortfall);
        if (expectedShortfall.compareTo(historicalValueAtRisk) < 0) {
            throw new IllegalArgumentException("expectedShortfall must not be less than historicalValueAtRisk");
        }
    }

    private static void requireMetric(String name, BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private static void requireRatio(String name, RiskAdjustedRatio ratio) {
        if (ratio == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private static void requireLossMetric(String name, BigDecimal value) {
        requireMetric(name, value);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
