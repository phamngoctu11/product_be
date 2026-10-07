package com.example.workflow.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionCalendarServiceTest {
    @Test
    void doesNotCountStartDateAndSkipsSunday() {
        ProductionCalendarService calendar = new ProductionCalendarService(Set.of());
        LocalDate saturday = LocalDate.of(2026, 10, 10);

        assertThat(calendar.calculateCompletionDate(saturday, 2))
                .isEqualTo(LocalDate.of(2026, 10, 13));
    }

    @Test
    void alsoSkipsConfiguredHoliday() {
        LocalDate mondayHoliday = LocalDate.of(2026, 10, 12);
        ProductionCalendarService calendar = new ProductionCalendarService(Set.of(mondayHoliday));

        assertThat(calendar.calculateCompletionDate(LocalDate.of(2026, 10, 10), 2))
                .isEqualTo(LocalDate.of(2026, 10, 14));
    }
}
