package com.example.workflow.util;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.Locale;

@UtilityClass
public class PaymentProviderNormalizer {
    private static final String DEFAULT_PROVIDER = "SEPAY";

    public String normalize(String provider) {
        return StringUtils.hasText(provider)
                ? provider.trim().toUpperCase(Locale.ROOT)
                : DEFAULT_PROVIDER;
    }
}
