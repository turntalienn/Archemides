package com.turntalienn.mytrade.analytics.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Nearest-rank historical simulation over the lower return tail.
 */
public final class HistoricalSimulationTailRiskModel implements TailRiskModel {

    @Override
    public TailRisk calculate(List<BigDecimal> returns, BigDecimal confidenceLevel) {
        List<BigDecimal> ordered = new ArrayList<>(returns);
        ordered.sort(Comparator.naturalOrder());

        BigDecimal tailProbability = BigDecimal.ONE.subtract(confidenceLevel, DecimalMath.CONTEXT);
        int tailCount = tailProbability
                .multiply(BigDecimal.valueOf(ordered.size()), DecimalMath.CONTEXT)
                .setScale(0, RoundingMode.CEILING)
                .intValueExact();
        tailCount = Math.max(1, Math.min(tailCount, ordered.size()));

        BigDecimal quantileReturn = ordered.get(tailCount - 1);
        BigDecimal tailSum = BigDecimal.ZERO;
        for (int index = 0; index < tailCount; index++) {
            tailSum = tailSum.add(ordered.get(index), DecimalMath.CONTEXT);
        }
        BigDecimal tailMean = tailSum.divide(BigDecimal.valueOf(tailCount), DecimalMath.CONTEXT);

        return new TailRisk(
                lossMagnitude(quantileReturn),
                lossMagnitude(tailMean)
        );
    }

    private static BigDecimal lossMagnitude(BigDecimal lowerTailReturn) {
        if (lowerTailReturn.signum() >= 0) {
            return BigDecimal.ZERO;
        }
        return DecimalMath.normalize(lowerTailReturn.negate(DecimalMath.CONTEXT));
    }
}
