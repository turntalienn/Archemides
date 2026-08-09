package com.turntalienn.mytrade.analytics.internal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy for lower-tail risk estimation.
 */
public interface TailRiskModel {

    TailRisk calculate(List<BigDecimal> returns, BigDecimal confidenceLevel);
}
