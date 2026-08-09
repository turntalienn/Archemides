package com.turntalienn.mytrade.analytics.api;

/**
 * Calculates portfolio performance from a chronological series of simple returns.
 */
public interface PortfolioAnalytics {

    PortfolioPerformanceReport analyze(ReturnSeries returnSeries, AnalyticsParameters parameters);
}
