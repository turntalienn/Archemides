package com.turntalienn.mytrade.runner.analytics;

import com.turntalienn.mytrade.analytics.api.AnalyticsParameters;
import com.turntalienn.mytrade.analytics.api.PortfolioAnalytics;
import com.turntalienn.mytrade.analytics.api.PortfolioPerformanceReport;
import com.turntalienn.mytrade.analytics.api.ReturnSeries;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.Objects;

/**
 * Observability decorator that keeps Micrometer out of the quantitative domain.
 */
final class ObservedPortfolioAnalytics implements PortfolioAnalytics {

    private final PortfolioAnalytics delegate;
    private final ObservationRegistry observationRegistry;

    ObservedPortfolioAnalytics(PortfolioAnalytics delegate, ObservationRegistry observationRegistry) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.observationRegistry = Objects.requireNonNull(observationRegistry, "observationRegistry must not be null");
    }

    @Override
    public PortfolioPerformanceReport analyze(ReturnSeries returnSeries, AnalyticsParameters parameters) {
        return Observation.createNotStarted("portfolio.analytics", observationRegistry)
                .lowCardinalityKeyValue("tail.risk.model", "historical-simulation")
                .observe(() -> delegate.analyze(returnSeries, parameters));
    }
}
