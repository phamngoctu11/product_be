package com.example.workflow.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.Locale;

@UtilityClass
public class TextNormalizer {

    public String optional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public String email(String value) {
        String normalized = optional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public String fullName(String lastName, String firstName) {
        String normalizedLastName = optional(lastName);
        String normalizedFirstName = optional(firstName);
        return ((normalizedLastName == null ? "" : normalizedLastName) + " "
                + (normalizedFirstName == null ? "" : normalizedFirstName)).trim();
    }
}
