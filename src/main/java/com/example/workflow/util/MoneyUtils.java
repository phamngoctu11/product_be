package com.example.workflow.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MoneyUtils {

    private static final double MONEY_ROUNDING_FACTOR = 100.0;

    public double round(double value) {
        return Math.round(value * MONEY_ROUNDING_FACTOR) / MONEY_ROUNDING_FACTOR;
    }
}
