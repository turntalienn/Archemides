package com.turntalienn.mytrade.analytics.api;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertThrows;

public class PortfolioPerformanceReportTest {

    @Test
    public void rejectsNegativeRiskAndVolatilityMetrics() {
        assertThrows(IllegalArgumentException.class, () -> report("-0.01", "0", "0", "0"));
        assertThrows(IllegalArgumentException.class, () -> report("0", "-0.01", "0", "0"));
        assertThrows(IllegalArgumentException.class, () -> report("0", "0", "-0.01", "0"));
        assertThrows(IllegalArgumentException.class, () -> report("0", "0", "0", "-0.01"));
    }

    @Test
    public void requiresExpectedShortfallToBeAtLeastValueAtRisk() {
        assertThrows(IllegalArgumentException.class, () -> report("0", "0.20", "0.10", "0.01"));
    }

    @Test
    public void enforcesDefinedAndUndefinedRatioStates() {
        assertThrows(IllegalArgumentException.class, () -> new RiskAdjustedRatio(
                null,
                RiskAdjustedRatio.Status.DEFINED
        ));
        assertThrows(IllegalArgumentException.class, () -> new RiskAdjustedRatio(
                BigDecimal.ZERO,
                RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR
        ));
        assertThrows(IllegalArgumentException.class, () -> new RiskAdjustedRatio(BigDecimal.ZERO, null));
    }

    private static PortfolioPerformanceReport report(
            String volatility,
            String valueAtRisk,
            String expectedShortfall,
            String drawdown
    ) {
        return new PortfolioPerformanceReport(
                2,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(volatility),
                RiskAdjustedRatio.defined(BigDecimal.ZERO),
                RiskAdjustedRatio.defined(BigDecimal.ZERO),
                new BigDecimal(drawdown),
                new BigDecimal(valueAtRisk),
                new BigDecimal(expectedShortfall)
        );
    }
}
