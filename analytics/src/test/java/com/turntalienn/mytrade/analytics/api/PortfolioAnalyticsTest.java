package com.turntalienn.mytrade.analytics.api;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PortfolioAnalyticsTest {

    private PortfolioAnalytics analytics;

    @Before
    public void setUp() {
        analytics = PortfolioAnalyticsFactory.historicalSimulation();
    }

    @Test
    public void calculatesCompoundedAndAnnualizedReturns() {
        PortfolioPerformanceReport report = analyze(
                returns("0.10", "0.10"),
                parameters(4, "0.75", "0", "0", "0")
        );

        assertDecimalEquals("0.21", report.totalReturn(), "0.000000000000001");
        assertDecimalEquals("0.4641", report.annualizedReturn(), "0.000000000000001");
        assertEquals(2, report.observations());
    }

    @Test
    public void appliesTheFisherEquationToInflation() {
        PortfolioPerformanceReport report = analyze(
                returns("0.10", "0"),
                parameters(2, "0.75", "0", "0.02", "0")
        );

        assertDecimalEquals("0.10", report.annualizedReturn(), "0.000000000000001");
        assertDecimalEquals("0.0784313725490196", report.realAnnualizedReturn(), "0.000000000000001");
    }

    @Test
    public void annualizesSampleVolatility() {
        PortfolioPerformanceReport report = analyze(
                returns("0.01", "-0.01"),
                parameters(2, "0.75", "0", "0", "0")
        );

        assertDecimalEquals("0.02", report.annualizedVolatility(), "0.000000000000001");
    }

    @Test
    public void usesTheGeometricPeriodicRiskFreeRateForSharpe() {
        PortfolioPerformanceReport zeroRiskFree = analyze(
                returns("0.10", "0"),
                parameters(2, "0.75", "0", "0", "0")
        );
        PortfolioPerformanceReport tenPercentPeriodicRiskFree = analyze(
                returns("0.20", "0"),
                parameters(2, "0.75", "0.21", "0", "0")
        );

        assertDecimalEquals("1", zeroRiskFree.sharpeRatio().value(), "0.000000000000001");
        assertDecimalEquals("0", tenPercentPeriodicRiskFree.sharpeRatio().value(), "0.000000000000001");
        assertEquals(RiskAdjustedRatio.Status.DEFINED, zeroRiskFree.sharpeRatio().status());
    }

    @Test
    public void calculatesTargetDownsideSortinoRatio() {
        PortfolioPerformanceReport zeroTarget = analyze(
                returns("0.10", "-0.05"),
                parameters(2, "0.75", "0", "0", "0")
        );
        PortfolioPerformanceReport positiveAnnualTarget = analyze(
                returns("0.10", "-0.05"),
                parameters(2, "0.75", "0", "0", "0.21")
        );

        assertDecimalEquals("1", zeroTarget.sortinoRatio().value(), "0.000000000000001");
        assertDecimalEquals("-1", positiveAnnualTarget.sortinoRatio().value(), "0.000000000000001");
    }

    @Test
    public void measuresMaximumPeakToTroughDrawdown() {
        PortfolioPerformanceReport report = analyze(
                returns("0.10", "-0.20", "0.10"),
                parameters(3, "0.75", "0", "0", "0")
        );

        assertDecimalEquals("0.20", report.maximumDrawdown(), "0.000000000000001");
    }

    @Test
    public void calculatesNearestRankHistoricalVarAndExpectedShortfallAsLosses() {
        PortfolioPerformanceReport report = analyze(
                returns("-0.20", "-0.10", "0.01", "0.02", "0.03"),
                parameters(5, "0.60", "0", "0", "0")
        );

        assertDecimalEquals("0.10", report.historicalValueAtRisk(), "0.000000000000001");
        assertDecimalEquals("0.15", report.expectedShortfall(), "0.000000000000001");
    }

    @Test
    public void reportsZeroLossMagnitudeForAnAllPositiveLowerTail() {
        PortfolioPerformanceReport report = analyze(
                returns("0.01", "0.02", "0.03"),
                parameters(3, "0.75", "0", "0", "0")
        );

        assertEquals(BigDecimal.ZERO, report.maximumDrawdown());
        assertEquals(BigDecimal.ZERO, report.historicalValueAtRisk());
        assertEquals(BigDecimal.ZERO, report.expectedShortfall());
    }

    @Test
    public void reportsZeroDispersionRatiosAsUndefined() {
        PortfolioPerformanceReport report = analyze(
                returns("0", "0", "0"),
                parameters(3, "0.75", "0", "0", "0")
        );

        assertEquals(BigDecimal.ZERO, report.annualizedVolatility());
        assertEquals(RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR, report.sharpeRatio().status());
        assertEquals(RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR, report.sortinoRatio().status());
        assertEquals(null, report.sharpeRatio().value());
        assertEquals(null, report.sortinoRatio().value());
    }

    @Test
    public void reportsConstantPositiveAndNegativeExcessSharpeRatiosAsUndefined() {
        PortfolioPerformanceReport positiveExcess = analyze(
                returns("0.01", "0.01", "0.01"),
                parameters(3, "0.75", "0", "0", "0")
        );
        PortfolioPerformanceReport negativeExcess = analyze(
                returns("0", "0", "0"),
                parameters(2, "0.75", "0.21", "0", "0")
        );

        assertEquals(RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR, positiveExcess.sharpeRatio().status());
        assertEquals(RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR, negativeExcess.sharpeRatio().status());
        assertEquals(RiskAdjustedRatio.Status.UNDEFINED_ZERO_DENOMINATOR, positiveExcess.sortinoRatio().status());
    }

    @Test
    public void annualizesLongGrowthAndLossSeriesWithoutOverflowOrUnderflow() {
        AnalyticsParameters parameters = parameters(252, "0.975", "0", "0", "0");
        BigDecimal gain = new BigDecimal("0.01");
        BigDecimal loss = new BigDecimal("-0.01");

        PortfolioPerformanceReport longGains = analyze(
                ReturnSeries.of(Collections.nCopies(100_000, gain)),
                parameters
        );
        PortfolioPerformanceReport oneGainYear = analyze(
                ReturnSeries.of(Collections.nCopies(252, gain)),
                parameters
        );
        PortfolioPerformanceReport longLosses = analyze(
                ReturnSeries.of(Collections.nCopies(100_000, loss)),
                parameters
        );
        PortfolioPerformanceReport oneLossYear = analyze(
                ReturnSeries.of(Collections.nCopies(252, loss)),
                parameters
        );

        assertDecimalEquals(oneGainYear.annualizedReturn(), longGains.annualizedReturn(), new BigDecimal("0.000000000001"));
        assertDecimalEquals(oneLossYear.annualizedReturn(), longLosses.annualizedReturn(), new BigDecimal("0.000000000001"));
    }

    @Test
    public void rejectsNullAnalysisArguments() {
        AnalyticsParameters parameters = parameters(2, "0.75", "0", "0", "0");
        ReturnSeries returns = returns("0", "0");

        assertThrows(IllegalArgumentException.class, () -> analytics.analyze(null, parameters));
        assertThrows(IllegalArgumentException.class, () -> analytics.analyze(returns, null));
    }

    private PortfolioPerformanceReport analyze(ReturnSeries series, AnalyticsParameters parameters) {
        return analytics.analyze(series, parameters);
    }

    private static ReturnSeries returns(String... values) {
        return ReturnSeries.of(Arrays.stream(values).map(BigDecimal::new).toList());
    }

    private static AnalyticsParameters parameters(
            int periods,
            String confidence,
            String riskFree,
            String inflation,
            String target
    ) {
        return new AnalyticsParameters(
                periods,
                new BigDecimal(confidence),
                new EconomicAssumptions(new BigDecimal(riskFree), new BigDecimal(inflation)),
                new BigDecimal(target)
        );
    }

    private static void assertDecimalEquals(String expected, BigDecimal actual, String tolerance) {
        assertDecimalEquals(new BigDecimal(expected), actual, new BigDecimal(tolerance));
    }

    private static void assertDecimalEquals(BigDecimal expected, BigDecimal actual, BigDecimal tolerance) {
        BigDecimal difference = actual.subtract(expected).abs();
        if (difference.compareTo(tolerance) > 0) {
            throw new AssertionError("expected " + expected + " but was " + actual + " (difference " + difference + ")");
        }
    }
}
