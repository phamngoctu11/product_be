package com.example.workflow.dto;

import com.example.workflow.nume.ChangeRequestDecision;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ChangeRequestDecisionRequest {
    @NotNull(message = "Phiên bản đơn hàng là bắt buộc.") private Long orderVersion;
    @NotNull(message = "Phiên bản yêu cầu chỉnh sửa là bắt buộc.") private Long changeRequestVersion;
    @NotNull(message = "Quyết định là bắt buộc.") private ChangeRequestDecision decision;
    @Size(max = 2000, message = "Lý do không được vượt quá 2000 ký tự.") private String reason;

    @JsonIgnore
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public ChangeRequestDecisionRequest() {
    }

    public ChangeRequestDecisionRequest(Long orderVersion, Long changeRequestVersion,
                                        ChangeRequestDecision decision, String reason) {
        this.orderVersion = orderVersion;
        this.changeRequestVersion = changeRequestVersion;
        this.decision = decision;
        this.reason = reason;
    }

    @JsonAnySetter
    public void captureUnknownField(String name, Object value) { unknownFields.put(name, value); }
    public boolean hasUnknownFields() { return !unknownFields.isEmpty(); }

    public Long orderVersion() { return orderVersion; }
    public Long changeRequestVersion() { return changeRequestVersion; }
    public ChangeRequestDecision decision() { return decision; }
    public String reason() { return reason; }
}
