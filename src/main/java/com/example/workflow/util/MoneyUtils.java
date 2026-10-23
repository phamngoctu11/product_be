package com.example.workflow.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MoneyUtils {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    public BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal nonNegative(BigDecimal value) {
        BigDecimal normalized = normalize(value);
        return normalized == null || normalized.signum() < 0 ? ZERO : normalized;
    }

    public BigDecimal subtractFloorZero(BigDecimal minuend, BigDecimal subtrahend) {
        BigDecimal result = normalize(minuend).subtract(normalize(subtrahend));
        return result.signum() < 0 ? ZERO : normalize(result);
    }
}
