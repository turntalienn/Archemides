package com.turntalienn.mytrade.analytics.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An immutable, chronologically ordered series of simple periodic returns.
 *
 * <p>A simple return must be greater than {@code -1}; a value of {@code -1}
 * would reduce wealth to zero and make subsequent compounded analytics
 * undefined.</p>
 */
public final class ReturnSeries {

    private static final BigDecimal MINIMUM_RETURN = BigDecimal.ONE.negate();

    private final List<BigDecimal> returns;

    private ReturnSeries(List<BigDecimal> returns) {
        this.returns = Collections.unmodifiableList(new ArrayList<>(returns));
    }

    /**
     * Creates a validated return series.
     *
     * @param returns chronological simple returns, expressed as decimals
     * @return an immutable defensive copy of the supplied values
     */
    public static ReturnSeries of(List<BigDecimal> returns) {
        if (returns == null) {
            throw new IllegalArgumentException("returns must not be null");
        }
        if (returns.size() < 2) {
            throw new IllegalArgumentException("at least two return observations are required");
        }
        for (int index = 0; index < returns.size(); index++) {
            BigDecimal value = returns.get(index);
            if (value == null) {
                throw new IllegalArgumentException("return at index " + index + " must not be null");
            }
            if (value.compareTo(MINIMUM_RETURN) <= 0) {
                throw new IllegalArgumentException("simple returns must be greater than -1");
            }
        }
        return new ReturnSeries(returns);
    }

    /**
     * Returns the immutable chronological observations.
     */
    public List<BigDecimal> returns() {
        return returns;
    }

    public int size() {
        return returns.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReturnSeries)) {
            return false;
        }
        ReturnSeries that = (ReturnSeries) other;
        return returns.equals(that.returns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returns);
    }

    @Override
    public String toString() {
        return "ReturnSeries" + returns;
    }
}
