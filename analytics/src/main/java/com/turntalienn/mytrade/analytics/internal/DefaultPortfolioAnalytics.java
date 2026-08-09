package com.turntalienn.mytrade.analytics.internal;

import com.turntalienn.mytrade.analytics.api.AnalyticsParameters;
import com.turntalienn.mytrade.analytics.api.EconomicAssumptions;
import com.turntalienn.mytrade.analytics.api.PortfolioAnalytics;
import com.turntalienn.mytrade.analytics.api.PortfolioPerformanceReport;
import com.turntalienn.mytrade.analytics.api.ReturnSeries;
import com.turntalienn.mytrade.analytics.api.RiskAdjustedRatio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic DECIMAL64 portfolio performance implementation.
 */
public final class DefaultPortfolioAnalytics implements PortfolioAnalytics {

    private final TailRiskModel tailRiskModel;

    public DefaultPortfolioAnalytics(TailRiskModel tailRiskModel) {
        if (tailRiskModel == null) {
            throw new IllegalArgumentException("tailRiskModel must not be null");
        }
        this.tailRiskModel = tailRiskModel;
    }

    @Override
    public PortfolioPerformanceReport analyze(ReturnSeries returnSeries, AnalyticsParameters parameters) {
        if (returnSeries == null) {
            throw new IllegalArgumentException("returnSeries must not be null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters must not be null");
        }

        List<BigDecimal> returns = returnSeries.returns();
        BigDecimal compoundedWealth = compoundedWealth(returns);
        BigDecimal totalReturn = compoundedWealth.subtract(BigDecimal.ONE, DecimalMath.CONTEXT);
        BigDecimal annualizedReturn = annualizedReturn(
                compoundedWealth,
                returns.size(),
                parameters.periodsPerYear()
        );
        BigDecimal realAnnualizedReturn = fisherRealReturn(
                annualizedReturn,
                parameters.economicAssumptions().annualInflationRate()
        );
        BigDecimal periodicVolatility = sampleStandardDeviation(returns);
        BigDecimal annualizationFactor = DecimalMath.squareRoot(BigDecimal.valueOf(parameters.periodsPerYear()));
        BigDecimal annualizedVolatility = periodicVolatility.multiply(annualizationFactor, DecimalMath.CONTEXT);

        RiskAdjustedRatio sharpeRatio = sharpeRatio(returns, periodicVolatility, annualizationFactor, parameters);
        RiskAdjustedRatio sortinoRatio = sortinoRatio(returns, annualizationFactor, parameters);
        BigDecimal maximumDrawdown = maximumDrawdown(returns);
        TailRisk tailRisk = tailRiskModel.calculate(returns, parameters.confidenceLevel());

        return new PortfolioPerformanceReport(
                returns.size(),
                DecimalMath.normalize(totalReturn),
                DecimalMath.normalize(annualizedReturn),
                DecimalMath.normalize(realAnnualizedReturn),
                DecimalMath.normalize(annualizedVolatility),
                sharpeRatio,
                sortinoRatio,
                DecimalMath.normalize(maximumDrawdown),
                DecimalMath.normalize(tailRisk.valueAtRisk()),
                DecimalMath.normalize(tailRisk.expectedShortfall())
        );
    }

    private static BigDecimal compoundedWealth(List<BigDecimal> returns) {
        BigDecimal wealth = BigDecimal.ONE;
        for (BigDecimal periodicReturn : returns) {
            wealth = wealth.multiply(BigDecimal.ONE.add(periodicReturn, DecimalMath.CONTEXT), DecimalMath.CONTEXT);
        }
        return wealth;
    }

    private static BigDecimal annualizedReturn(BigDecimal compoundedWealth, int observations, int periodsPerYear) {
        double exponent = (double) periodsPerYear / observations;
        return DecimalMath.power(compoundedWealth, exponent).subtract(BigDecimal.ONE, DecimalMath.CONTEXT);
    }

    private static BigDecimal fisherRealReturn(BigDecimal nominalReturn, BigDecimal annualInflationRate) {
        BigDecimal nominalGrowth = BigDecimal.ONE.add(nominalReturn, DecimalMath.CONTEXT);
        BigDecimal inflationGrowth = BigDecimal.ONE.add(annualInflationRate, DecimalMath.CONTEXT);
        return nominalGrowth.divide(inflationGrowth, DecimalMath.CONTEXT)
                .subtract(BigDecimal.ONE, DecimalMath.CONTEXT);
    }

    private static BigDecimal sampleStandardDeviation(List<BigDecimal> values) {
        BigDecimal average = mean(values);
        BigDecimal squaredDeviations = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            BigDecimal deviation = value.subtract(average, DecimalMath.CONTEXT);
            squaredDeviations = squaredDeviations.add(
                    deviation.multiply(deviation, DecimalMath.CONTEXT),
                    DecimalMath.CONTEXT
            );
        }
        BigDecimal sampleVariance = squaredDeviations.divide(
                BigDecimal.valueOf(values.size() - 1L),
                DecimalMath.CONTEXT
        );
        return DecimalMath.squareRoot(sampleVariance);
    }

    private static RiskAdjustedRatio sharpeRatio(
            List<BigDecimal> returns,
            BigDecimal periodicVolatility,
            BigDecimal annualizationFactor,
            AnalyticsParameters parameters
    ) {
        if (periodicVolatility.signum() == 0) {
            return RiskAdjustedRatio.undefinedZeroDenominator();
        }
        EconomicAssumptions assumptions = parameters.economicAssumptions();
        BigDecimal periodicRiskFreeRate = DecimalMath.periodicRate(
                assumptions.annualRiskFreeRate(),
                parameters.periodsPerYear()
        );
        BigDecimal meanExcessReturn = mean(returns).subtract(periodicRiskFreeRate, DecimalMath.CONTEXT);
        BigDecimal value = meanExcessReturn.divide(periodicVolatility, DecimalMath.CONTEXT)
                .multiply(annualizationFactor, DecimalMath.CONTEXT);
        return RiskAdjustedRatio.defined(DecimalMath.normalize(value));
    }

    private static RiskAdjustedRatio sortinoRatio(
            List<BigDecimal> returns,
            BigDecimal annualizationFactor,
            AnalyticsParameters parameters
    ) {
        BigDecimal periodicTarget = DecimalMath.periodicRate(
                parameters.annualTargetReturn(),
                parameters.periodsPerYear()
        );
        List<BigDecimal> excessReturns = new ArrayList<>(returns.size());
        BigDecimal downsideSquares = BigDecimal.ZERO;
        for (BigDecimal periodicReturn : returns) {
            BigDecimal excess = periodicReturn.subtract(periodicTarget, DecimalMath.CONTEXT);
            excessReturns.add(excess);
            if (excess.signum() < 0) {
                downsideSquares = downsideSquares.add(
                        excess.multiply(excess, DecimalMath.CONTEXT),
                        DecimalMath.CONTEXT
                );
            }
        }
        BigDecimal downsideVariance = downsideSquares.divide(
                BigDecimal.valueOf(returns.size()),
                DecimalMath.CONTEXT
        );
        BigDecimal downsideDeviation = DecimalMath.squareRoot(downsideVariance);
        if (downsideDeviation.signum() == 0) {
            return RiskAdjustedRatio.undefinedZeroDenominator();
        }
        BigDecimal value = mean(excessReturns).divide(downsideDeviation, DecimalMath.CONTEXT)
                .multiply(annualizationFactor, DecimalMath.CONTEXT);
        return RiskAdjustedRatio.defined(DecimalMath.normalize(value));
    }

    private static BigDecimal maximumDrawdown(List<BigDecimal> returns) {
        BigDecimal wealth = BigDecimal.ONE;
        BigDecimal peak = BigDecimal.ONE;
        BigDecimal maximumDrawdown = BigDecimal.ZERO;
        for (BigDecimal periodicReturn : returns) {
            wealth = wealth.multiply(BigDecimal.ONE.add(periodicReturn, DecimalMath.CONTEXT), DecimalMath.CONTEXT);
            if (wealth.compareTo(peak) > 0) {
                peak = wealth;
            }
            BigDecimal drawdown = peak.subtract(wealth, DecimalMath.CONTEXT)
                    .divide(peak, DecimalMath.CONTEXT);
            if (drawdown.compareTo(maximumDrawdown) > 0) {
                maximumDrawdown = drawdown;
            }
        }
        return maximumDrawdown;
    }

    private static BigDecimal mean(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value, DecimalMath.CONTEXT);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), DecimalMath.CONTEXT);
    }
}
