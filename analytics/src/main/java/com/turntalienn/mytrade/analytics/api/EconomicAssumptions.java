package com.turntalienn.mytrade.analytics.api;

import java.math.BigDecimal;

/**
 * Annual economic rates used to make performance comparable in real and
 * risk-adjusted terms.
 */
public record EconomicAssumptions(
        BigDecimal annualRiskFreeRate,
        BigDecimal annualInflationRate
) {

    private static final BigDecimal MINIMUM_RATE = BigDecimal.ONE.negate();

    public EconomicAssumptions {
        validateRate("annualRiskFreeRate", annualRiskFreeRate);
        validateRate("annualInflationRate", annualInflationRate);
    }

    private static void validateRate(String name, BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (value.compareTo(MINIMUM_RATE) <= 0) {
            throw new IllegalArgumentException(name + " must be greater than -1");
        }
    }
}
