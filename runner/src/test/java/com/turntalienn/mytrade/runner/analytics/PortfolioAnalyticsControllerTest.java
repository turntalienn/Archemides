package com.turntalienn.mytrade.runner.analytics;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void calculatesRiskReturnAndEconomicMetrics() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodicReturns": [0.10, -0.05, 0.02, -0.01],
                                  "periodsPerYear": 12,
                                  "confidenceLevel": 0.95,
                                  "annualRiskFreeRate": 0.04,
                                  "annualInflationRate": 0.03,
                                  "annualTargetReturn": 0.00
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observations").value(4))
                .andExpect(jsonPath("$.totalReturn").isNumber())
                .andExpect(jsonPath("$.realAnnualizedReturn").isNumber())
                .andExpect(jsonPath("$.annualizedVolatility").isNumber())
                .andExpect(jsonPath("$.sharpeRatio.value").isNumber())
                .andExpect(jsonPath("$.sharpeRatio.status").value("DEFINED"))
                .andExpect(jsonPath("$.sortinoRatio.value").isNumber())
                .andExpect(jsonPath("$.sortinoRatio.status").value("DEFINED"))
                .andExpect(jsonPath("$.maximumDrawdown").isNumber())
                .andExpect(jsonPath("$.historicalValueAtRisk").value(0.05))
                .andExpect(jsonPath("$.expectedShortfall").value(0.05));

        assertThat(meterRegistry.find("portfolio.analytics").timer()).isNotNull();
    }

    @Test
    void rejectsReturnsThatWouldLoseMoreThanThePortfolio() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodicReturns": [0.02, -1.00],
                                  "periodsPerYear": 252,
                                  "confidenceLevel": 0.975,
                                  "annualRiskFreeRate": 0.04,
                                  "annualInflationRate": 0.03
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explicitlyReportsRatiosWithNoRiskDenominatorAsUndefined() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/performance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodicReturns": [0.01, 0.01],
                                  "periodsPerYear": 252,
                                  "confidenceLevel": 0.975,
                                  "annualRiskFreeRate": 0,
                                  "annualInflationRate": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharpeRatio.status").value("UNDEFINED_ZERO_DENOMINATOR"))
                .andExpect(jsonPath("$.sharpeRatio.value").value(nullValue()))
                .andExpect(jsonPath("$.sortinoRatio.status").value("UNDEFINED_ZERO_DENOMINATOR"))
                .andExpect(jsonPath("$.sortinoRatio.value").value(nullValue()));
    }

    @Test
    void exposesAProductionHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
