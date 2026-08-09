package com.turntalienn.mytrade.analytics.internal;

import com.turntalienn.mytrade.analytics.api.AnalyticsParameters;
import com.turntalienn.mytrade.analytics.api.EconomicAssumptions;
import com.turntalienn.mytrade.analytics.api.PortfolioPerformanceReport;
import com.turntalienn.mytrade.analytics.api.ReturnSeries;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DefaultPortfolioAnalyticsStrategyTest {

    @Test
    public void delegatesTailMetricsToTheInjectedStrategy() {
        TailRiskModel strategy = (returns, confidence) -> new TailRisk(
                new BigDecimal("0.123"),
                new BigDecimal("0.456")
        );
        DefaultPortfolioAnalytics analytics = new DefaultPortfolioAnalytics(strategy);

        PortfolioPerformanceReport report = analytics.analyze(
                ReturnSeries.of(Arrays.asList(BigDecimal.ZERO, BigDecimal.ZERO)),
                new AnalyticsParameters(
                        2,
                        new BigDecimal("0.95"),
                        new EconomicAssumptions(BigDecimal.ZERO, BigDecimal.ZERO)
                )
        );

        assertEquals(new BigDecimal("0.123"), report.historicalValueAtRisk());
        assertEquals(new BigDecimal("0.456"), report.expectedShortfall());
    }

    @Test
    public void requiresATailRiskStrategy() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultPortfolioAnalytics(null));
    }

    @Test
    public void historicalStrategyUsesCeilingForFractionalTailCounts() {
        HistoricalSimulationTailRiskModel model = new HistoricalSimulationTailRiskModel();
        List<BigDecimal> returns = Arrays.asList(
                new BigDecimal("-0.30"),
                new BigDecimal("-0.20"),
                new BigDecimal("-0.10"),
                BigDecimal.ZERO,
                new BigDecimal("0.10"),
                new BigDecimal("0.20")
        );

        TailRisk risk = model.calculate(returns, new BigDecimal("0.75"));

        assertEquals(new BigDecimal("0.20"), risk.valueAtRisk());
        assertEquals(new BigDecimal("0.25"), risk.expectedShortfall());
    }
}
