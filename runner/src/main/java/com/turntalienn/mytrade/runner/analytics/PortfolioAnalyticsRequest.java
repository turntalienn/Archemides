package com.turntalienn.mytrade.runner.analytics;

import com.turntalienn.mytrade.analytics.api.AnalyticsParameters;
import com.turntalienn.mytrade.analytics.api.EconomicAssumptions;
import com.turntalienn.mytrade.analytics.api.ReturnSeries;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAnalyticsRequest(
        @NotNull
        @Size(min = 2, max = 100_000)
        List<@NotNull @DecimalMin(value = "-1", inclusive = false) BigDecimal> periodicReturns,

        @NotNull
        @Min(1)
        @Max(1_000_000)
        Integer periodsPerYear,

        @NotNull
        @DecimalMin(value = "0.5", inclusive = false)
        @DecimalMax(value = "1", inclusive = false)
        BigDecimal confidenceLevel,

        @NotNull
        @DecimalMin(value = "-1", inclusive = false)
        BigDecimal annualRiskFreeRate,

        @NotNull
        @DecimalMin(value = "-1", inclusive = false)
        BigDecimal annualInflationRate,

        @DecimalMin(value = "-1", inclusive = false)
        BigDecimal annualTargetReturn
) {

    ReturnSeries toReturnSeries() {
        return ReturnSeries.of(periodicReturns);
    }

    AnalyticsParameters toParameters() {
        EconomicAssumptions assumptions = new EconomicAssumptions(
                annualRiskFreeRate,
                annualInflationRate
        );
        BigDecimal targetReturn = annualTargetReturn == null ? BigDecimal.ZERO : annualTargetReturn;
        return new AnalyticsParameters(periodsPerYear, confidenceLevel, assumptions, targetReturn);
    }
}
