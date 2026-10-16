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
    @NotNull(message = "orderVersion is required") private Long orderVersion;
    @NotNull(message = "changeRequestVersion is required") private Long changeRequestVersion;
    @NotNull(message = "decision is required") private ChangeRequestDecision decision;
    @Size(max = 2000, message = "reason must be at most 2000 characters") private String reason;

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
