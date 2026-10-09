package com.example.workflow.entity.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringListJsonConverterTest {
    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void roundTripsImageUrlsInOrder() {
        List<String> urls = List.of("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg");

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(urls)))
                .containsExactlyElementsOf(urls);
    }

    @Test
    void readsNullAndLegacyRawAttachmentSafely() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("https://legacy.example.com/image.jpg"))
                .containsExactly("https://legacy.example.com/image.jpg");
    }
}
