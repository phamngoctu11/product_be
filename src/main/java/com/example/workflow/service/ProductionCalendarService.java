package com.example.workflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductionCalendarService {
    private final Set<LocalDate> holidays;

    @Autowired
    public ProductionCalendarService(@Value("${workflow.production.holidays:}") String configuredHolidays) {
        this(parseHolidays(configuredHolidays));
    }

    ProductionCalendarService(Set<LocalDate> holidays) {
        this.holidays = holidays == null ? Set.of() : Set.copyOf(holidays);
    }

    /** The start date is not counted. Sundays and configured holidays are skipped. */
    public LocalDate calculateCompletionDate(LocalDate startDate, int productionDays) {
        if (startDate == null) throw new IllegalArgumentException("Start date is required");
        if (productionDays < 1) throw new IllegalArgumentException("Production days must be positive");

        LocalDate cursor = startDate;
        int countedDays = 0;
        while (countedDays < productionDays) {
            cursor = cursor.plusDays(1);
            if (isProductionDay(cursor)) countedDays++;
        }
        return cursor;
    }

    public boolean isProductionDay(LocalDate date) {
        return date != null && date.getDayOfWeek() != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

    private static Set<LocalDate> parseHolidays(String configuredHolidays) {
        if (!StringUtils.hasText(configuredHolidays)) return Set.of();
        return Arrays.stream(configuredHolidays.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }
}
