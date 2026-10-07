package com.example.workflow.service;

import com.example.workflow.exception.AppException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogDurationCalculatorTest {
    private final CatalogDurationCalculator calculator = new CatalogDurationCalculator();

    @ParameterizedTest
    @CsvSource({"1,5", "5,14", "6,16", "10,22"})
    void calculatesApprovedCatalogExamples(int quantity, int expectedDays) {
        assertThat(calculator.calculate(3.0, quantity)).isEqualTo(expectedDays);
    }

    @Test
    void roundsMakeDaysAndQualityAllowanceSeparately() {
        assertThat(calculator.calculate(2.0, 1)).isEqualTo(4);
    }

    @Test
    void rejectsMissingOrInvalidMadeDay() {
        assertThatThrownBy(() -> calculator.calculate(null, 1)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> calculator.calculate(1.9, 1)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> calculator.calculate(Double.NaN, 1)).isInstanceOf(AppException.class);
    }
}
