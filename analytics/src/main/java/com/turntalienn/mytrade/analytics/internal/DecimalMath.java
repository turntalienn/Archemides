package com.turntalienn.mytrade.analytics.internal;

import java.math.BigDecimal;
import java.math.MathContext;

final class DecimalMath {

    static final MathContext CONTEXT = MathContext.DECIMAL64;
    static final BigDecimal ZERO = BigDecimal.ZERO;
    static final BigDecimal ONE = BigDecimal.ONE;

    private DecimalMath() {
    }

    static BigDecimal fromDouble(double value) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("analytics result is not finite");
        }
        if (value == 0.0d) {
            return ZERO;
        }
        return BigDecimal.valueOf(value).round(CONTEXT);
    }

    static BigDecimal power(BigDecimal base, double exponent) {
        if (base.signum() <= 0) {
            throw new ArithmeticException("power base must be positive");
        }
        if (!Double.isFinite(exponent)) {
            throw new ArithmeticException("power exponent must be finite");
        }
        if (exponent == 0.0d || base.compareTo(ONE) == 0) {
            return ONE;
        }

        BigDecimal normalized = base.stripTrailingZeros();
        long sourcePower = (long) normalized.precision() - normalized.scale() - 1L;
        if (sourcePower < Integer.MIN_VALUE + 1L || sourcePower > Integer.MAX_VALUE) {
            throw new ArithmeticException("power base is outside the supported decimal range");
        }

        BigDecimal significand = normalized.scaleByPowerOfTen(-(int) sourcePower);
        double resultLog10 = (Math.log10(significand.doubleValue()) + sourcePower) * exponent;
        if (!Double.isFinite(resultLog10)) {
            throw new ArithmeticException("analytics result is not finite");
        }

        double flooredPower = Math.floor(resultLog10);
        if (flooredPower < Integer.MIN_VALUE + 16.0d || flooredPower > Integer.MAX_VALUE - 16.0d) {
            throw new ArithmeticException("analytics result is outside the supported decimal range");
        }
        int resultPower = (int) flooredPower;
        double resultSignificand = Math.pow(10.0d, resultLog10 - flooredPower);
        return BigDecimal.valueOf(resultSignificand)
                .scaleByPowerOfTen(resultPower)
                .round(CONTEXT);
    }

    static BigDecimal squareRoot(BigDecimal value) {
        if (value.signum() < 0) {
            throw new ArithmeticException("cannot take the square root of a negative value");
        }
        return fromDouble(Math.sqrt(value.doubleValue()));
    }

    static BigDecimal periodicRate(BigDecimal annualRate, int periodsPerYear) {
        BigDecimal growthFactor = ONE.add(annualRate, CONTEXT);
        return power(growthFactor, 1.0d / periodsPerYear).subtract(ONE, CONTEXT);
    }

    static BigDecimal normalize(BigDecimal value) {
        BigDecimal rounded = value.round(CONTEXT);
        return rounded.signum() == 0 ? ZERO : rounded;
    }
}
