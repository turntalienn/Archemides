package com.turntalienn.mytrade.analytics.api;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AnalyticsParametersTest {

    private static final EconomicAssumptions ECONOMICS = new EconomicAssumptions(
            new BigDecimal("0.04"),
            new BigDecimal("0.02")
    );

    @Test
    public void validatesEconomicRates() {
        assertThrows(IllegalArgumentException.class, () -> new EconomicAssumptions(null, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new EconomicAssumptions(BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> new EconomicAssumptions(new BigDecimal("-1"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new EconomicAssumptions(BigDecimal.ZERO, new BigDecimal("-1.1")));

        EconomicAssumptions assumptions = new EconomicAssumptions(
                new BigDecimal("-0.999"),
                new BigDecimal("-0.25")
        );
        assertEquals(new BigDecimal("-0.999"), assumptions.annualRiskFreeRate());
    }

    @Test
    public void requiresPositivePeriodsPerYear() {
        assertThrows(IllegalArgumentException.class, () -> parameters(0, "0.95", "0"));
        assertThrows(IllegalArgumentException.class, () -> parameters(-1, "0.95", "0"));
    }

    @Test
    public void requiresConfidenceStrictlyBetweenHalfAndOne() {
        assertThrows(IllegalArgumentException.class, () -> parameters(252, "0.5", "0"));
        assertThrows(IllegalArgumentException.class, () -> parameters(252, "1", "0"));
        assertThrows(IllegalArgumentException.class, () -> parameters(252, "0.49", "0"));
        assertThrows(IllegalArgumentException.class, () -> parameters(252, "1.01", "0"));
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsParameters(252, null, ECONOMICS));
    }

    @Test
    public void validatesDependenciesAndAnnualTarget() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsParameters(
                252,
                new BigDecimal("0.95"),
                null,
                BigDecimal.ZERO
        ));
        assertThrows(IllegalArgumentException.class, () -> new AnalyticsParameters(
                252,
                new BigDecimal("0.95"),
                ECONOMICS,
                null
        ));
        assertThrows(IllegalArgumentException.class, () -> parameters(252, "0.95", "-1"));
    }

    @Test
    public void convenienceConstructorUsesAZeroDownsideTarget() {
        AnalyticsParameters parameters = new AnalyticsParameters(252, new BigDecimal("0.95"), ECONOMICS);

        assertEquals(BigDecimal.ZERO, parameters.annualTargetReturn());
    }

    private static AnalyticsParameters parameters(int periods, String confidence, String target) {
        return new AnalyticsParameters(
                periods,
                new BigDecimal(confidence),
                ECONOMICS,
                new BigDecimal(target)
        );
    }
}
