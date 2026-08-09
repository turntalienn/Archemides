package com.turntalienn.mytrade.analytics.internal;

import java.math.BigDecimal;

public record TailRisk(BigDecimal valueAtRisk, BigDecimal expectedShortfall) {
}
