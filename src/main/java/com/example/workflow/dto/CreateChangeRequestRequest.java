package com.example.workflow.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class CreateChangeRequestRequest {
    @NotNull(message = "Phiên bản đơn hàng là bắt buộc.") private Long orderVersion;
    @NotNull(message = "Mã sản phẩm trong đơn hàng là bắt buộc.") private Long orderItemId;
    @NotNull(message = "Phiên bản thông số gốc là bắt buộc.") private Integer baseSpecVersion;
    @NotBlank(message = "Mô tả đề xuất là bắt buộc.")
    @Size(max = 4000, message = "Mô tả đề xuất không được vượt quá 4000 ký tự.")
    private String proposedDescription;
    private Map<String, Object> fieldValues;
    @NotBlank(message = "Vui lòng nhập lý do.")
    @Size(max = 2000, message = "Lý do không được vượt quá 2000 ký tự.")
    private String reason;

    @JsonIgnore
    private final Map<String, Object> unknownFields = new LinkedHashMap<>();

    public CreateChangeRequestRequest() {
    }

    public CreateChangeRequestRequest(Long orderVersion, Long orderItemId, Integer baseSpecVersion,
                                      String proposedDescription, Map<String, Object> fieldValues, String reason) {
        this.orderVersion = orderVersion;
        this.orderItemId = orderItemId;
        this.baseSpecVersion = baseSpecVersion;
        this.proposedDescription = proposedDescription;
        this.fieldValues = fieldValues;
        this.reason = reason;
    }

    @JsonAnySetter
    public void captureUnknownField(String name, Object value) { unknownFields.put(name, value); }
    public boolean hasUnknownFields() { return !unknownFields.isEmpty(); }

    public Long orderVersion() { return orderVersion; }
    public Long orderItemId() { return orderItemId; }
    public Integer baseSpecVersion() { return baseSpecVersion; }
    public String proposedDescription() { return proposedDescription; }
    public Map<String, Object> fieldValues() { return fieldValues; }
    public String reason() { return reason; }
}
