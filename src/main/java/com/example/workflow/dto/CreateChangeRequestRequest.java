package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CreateChangeRequestRequest {
    @NotNull(message = "orderVersion is required") private Long orderVersion;
    @NotNull(message = "orderItemId is required") private Long orderItemId;
    @NotNull(message = "baseSpecVersion is required") private Integer baseSpecVersion;
    @NotBlank(message = "proposedDescription is required")
    @Size(max = 4000, message = "proposedDescription must be at most 4000 characters")
    private String proposedDescription;
    @Size(max = 10, message = "At most 10 images are allowed") private List<String> proposedImageUrls;
    @NotBlank(message = "reason is required")
    @Size(max = 2000, message = "reason must be at most 2000 characters")
    private String reason;

    @JsonIgnore
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public CreateChangeRequestRequest() {
    }

    public CreateChangeRequestRequest(Long orderVersion, Long orderItemId, Integer baseSpecVersion,
                                      String proposedDescription, List<String> proposedImageUrls, String reason) {
        this.orderVersion = orderVersion;
        this.orderItemId = orderItemId;
        this.baseSpecVersion = baseSpecVersion;
        this.proposedDescription = proposedDescription;
        this.proposedImageUrls = proposedImageUrls;
        this.reason = reason;
    }

    @JsonAnySetter
    public void captureUnknownField(String name, Object value) { unknownFields.put(name, value); }
    public boolean hasUnknownFields() { return !unknownFields.isEmpty(); }

    public Long orderVersion() { return orderVersion; }
    public Long orderItemId() { return orderItemId; }
    public Integer baseSpecVersion() { return baseSpecVersion; }
    public String proposedDescription() { return proposedDescription; }
    public List<String> proposedImageUrls() { return proposedImageUrls; }
    public String reason() { return reason; }
}
