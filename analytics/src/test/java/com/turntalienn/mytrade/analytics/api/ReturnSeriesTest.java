package com.turntalienn.mytrade.analytics.api;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public class ReturnSeriesTest {

    @Test
    public void createsAnImmutableDefensiveCopy() {
        List<BigDecimal> source = new ArrayList<>(Arrays.asList(
                new BigDecimal("0.10"),
                new BigDecimal("-0.05")
        ));

        ReturnSeries series = ReturnSeries.of(source);
        source.set(0, BigDecimal.ZERO);

        assertEquals(new BigDecimal("0.10"), series.returns().get(0));
        assertEquals(2, series.size());
        assertThrows(UnsupportedOperationException.class, () -> series.returns().add(BigDecimal.ZERO));
    }

    @Test
    public void comparesByItsOrderedObservations() {
        ReturnSeries first = ReturnSeries.of(Arrays.asList(new BigDecimal("0.1"), new BigDecimal("0.2")));
        ReturnSeries equal = ReturnSeries.of(Arrays.asList(new BigDecimal("0.1"), new BigDecimal("0.2")));
        ReturnSeries reordered = ReturnSeries.of(Arrays.asList(new BigDecimal("0.2"), new BigDecimal("0.1")));

        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertNotEquals(first, reordered);
    }

    @Test
    public void rejectsFewerThanTwoObservations() {
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(List.of()));
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(List.of(BigDecimal.ZERO)));
    }

    @Test
    public void rejectsNullInputAndNullObservations() {
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(null));
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(Arrays.asList(BigDecimal.ZERO, null)));
    }

    @Test
    public void rejectsACompleteOrGreaterLoss() {
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(Arrays.asList(
                BigDecimal.ZERO,
                new BigDecimal("-1")
        )));
        assertThrows(IllegalArgumentException.class, () -> ReturnSeries.of(Arrays.asList(
                BigDecimal.ZERO,
                new BigDecimal("-1.01")
        )));
    }
}
