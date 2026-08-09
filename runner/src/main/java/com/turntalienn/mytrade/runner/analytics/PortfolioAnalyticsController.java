package com.turntalienn.mytrade.runner.analytics;

import com.turntalienn.mytrade.analytics.api.PortfolioAnalytics;
import com.turntalienn.mytrade.analytics.api.PortfolioPerformanceReport;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
class PortfolioAnalyticsController {

    private final PortfolioAnalytics portfolioAnalytics;

    PortfolioAnalyticsController(PortfolioAnalytics portfolioAnalytics) {
        this.portfolioAnalytics = portfolioAnalytics;
    }

    @PostMapping("/performance")
    PortfolioPerformanceReport analyze(@Valid @RequestBody PortfolioAnalyticsRequest request) {
        return portfolioAnalytics.analyze(request.toReturnSeries(), request.toParameters());
    }
}
