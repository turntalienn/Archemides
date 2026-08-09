package com.turntalienn.mytrade.analytics.api;

import com.turntalienn.mytrade.analytics.internal.DefaultPortfolioAnalytics;
import com.turntalienn.mytrade.analytics.internal.HistoricalSimulationTailRiskModel;

/**
 * Public construction boundary for analytics implementations.
 */
public final class PortfolioAnalyticsFactory {

    private PortfolioAnalyticsFactory() {
    }

    public static PortfolioAnalytics historicalSimulation() {
        return new DefaultPortfolioAnalytics(new HistoricalSimulationTailRiskModel());
    }
}
