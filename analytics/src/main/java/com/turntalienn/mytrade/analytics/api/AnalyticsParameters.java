package com.turntalienn.mytrade.analytics.api;

import java.math.BigDecimal;

/**
 * Sampling and economic parameters for portfolio analytics.
 *
 * @param periodsPerYear number of observations expected in one year
 * @param confidenceLevel confidence used for historical VaR and expected shortfall
 * @param economicAssumptions annual risk-free and inflation assumptions
 * @param annualTargetReturn annual minimum acceptable return for the Sortino ratio
 */
public record AnalyticsParameters(
        int periodsPerYear,
        BigDecimal confidenceLevel,
        EconomicAssumptions economicAssumptions,
        BigDecimal annualTargetReturn
) {

    private static final BigDecimal HALF = new BigDecimal("0.5");
    private static final BigDecimal MINIMUM_RATE = BigDecimal.ONE.negate();

    public AnalyticsParameters {
        if (periodsPerYear <= 0) {
            throw new IllegalArgumentException("periodsPerYear must be greater than zero");
        }
        if (confidenceLevel == null) {
            throw new IllegalArgumentException("confidenceLevel must not be null");
        }
        if (confidenceLevel.compareTo(HALF) <= 0 || confidenceLevel.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("confidenceLevel must be strictly between 0.5 and 1");
        }
        if (economicAssumptions == null) {
            throw new IllegalArgumentException("economicAssumptions must not be null");
        }
        if (annualTargetReturn == null) {
            throw new IllegalArgumentException("annualTargetReturn must not be null");
        }
        if (annualTargetReturn.compareTo(MINIMUM_RATE) <= 0) {
            throw new IllegalArgumentException("annualTargetReturn must be greater than -1");
        }
    }

    /**
     * Convenience constructor for a zero annual downside target.
     */
    public AnalyticsParameters(
            int periodsPerYear,
            BigDecimal confidenceLevel,
            EconomicAssumptions economicAssumptions
    ) {
        this(periodsPerYear, confidenceLevel, economicAssumptions, BigDecimal.ZERO);
    }
}
