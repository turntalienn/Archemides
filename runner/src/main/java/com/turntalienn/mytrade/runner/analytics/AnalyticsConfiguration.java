package com.turntalienn.mytrade.runner.analytics;

import com.turntalienn.mytrade.analytics.api.PortfolioAnalytics;
import com.turntalienn.mytrade.analytics.api.PortfolioAnalyticsFactory;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AnalyticsConfiguration {

    @Bean
    PortfolioAnalytics portfolioAnalytics(ObservationRegistry observationRegistry) {
        PortfolioAnalytics analytics = PortfolioAnalyticsFactory.historicalSimulation();
        return new ObservedPortfolioAnalytics(analytics, observationRegistry);
    }
}
