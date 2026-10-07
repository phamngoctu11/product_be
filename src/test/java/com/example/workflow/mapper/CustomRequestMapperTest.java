package com.example.workflow.mapper;

import com.example.workflow.entity.CustomRequest;
import com.example.workflow.nume.CustomRequestStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CustomRequestMapperTest {
    private final CustomRequestMapper mapper = Mappers.getMapper(CustomRequestMapper.class);

    @Test
    void mapsEditableDraftWithoutExposingOwnerId() {
        CustomRequest request = request(CustomRequestStatus.DRAFT);

        var result = mapper.toDto(request);

        assertThat(result.id()).isEqualTo(12L);
        assertThat(result.version()).isEqualTo(3L);
        assertThat(result.status()).isEqualTo(CustomRequestStatus.DRAFT);
        assertThat(result.spec()).isEqualTo("{\"material\":\"wood\"}");
        assertThat(result.attachments()).isEqualTo("[\"reference-1\"]");
        assertThat(result.quantity()).isEqualTo(2);
        assertThat(result.editable()).isTrue();
        assertThat(result.linkedOrderId()).isNull();
    }

    @Test
    void mapsSubmittedDraftAsReadOnly() {
        CustomRequest request = request(CustomRequestStatus.DRAFT);
        LocalDateTime submittedAt = LocalDateTime.of(2026, 10, 7, 10, 30);
        request.markSubmitted(99L, submittedAt);

        var result = mapper.toDto(request);

        assertThat(result.status()).isEqualTo(CustomRequestStatus.SUBMITTED);
        assertThat(result.linkedOrderId()).isEqualTo(99L);
        assertThat(result.submittedAt()).isEqualTo(submittedAt);
        assertThat(result.editable()).isFalse();
    }

    private CustomRequest request(CustomRequestStatus status) {
        CustomRequest request = new CustomRequest();
        request.setId(12L);
        request.setVersion(3L);
        request.setOwnerId("owner-1");
        request.setStatus(status);
        request.setSpec("{\"material\":\"wood\"}");
        request.setAttachments("[\"reference-1\"]");
        request.setQuantity(2);
        request.setSourceOrderId(11L);
        request.setCreatedAt(LocalDateTime.of(2026, 10, 7, 9, 0));
        request.setUpdatedAt(LocalDateTime.of(2026, 10, 7, 9, 15));
        return request;
    }
}
