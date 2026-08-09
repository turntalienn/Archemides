package com.turntalienn.mytrade.analytics.api;

import java.math.BigDecimal;

/**
 * A risk-adjusted ratio whose mathematical availability is explicit.
 *
 * <p>Ratios such as Sharpe and Sortino are undefined when their risk
 * denominator is zero. Returning that state prevents a constant positive
 * return series from being misreported as zero performance.</p>
 */
public record RiskAdjustedRatio(BigDecimal value, Status status) {

    public enum Status {
        DEFINED,
        UNDEFINED_ZERO_DENOMINATOR
    }

    public RiskAdjustedRatio {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status == Status.DEFINED && value == null) {
            throw new IllegalArgumentException("a defined ratio must have a value");
        }
        if (status == Status.UNDEFINED_ZERO_DENOMINATOR && value != null) {
            throw new IllegalArgumentException("an undefined ratio must not have a value");
        }
    }

    public static RiskAdjustedRatio defined(BigDecimal value) {
        return new RiskAdjustedRatio(value, Status.DEFINED);
    }

    public static RiskAdjustedRatio undefinedZeroDenominator() {
        return new RiskAdjustedRatio(null, Status.UNDEFINED_ZERO_DENOMINATOR);
    }
}
