package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationReviewDTO {
    private Long id;
    private Long attributionId;
    private Long consultationRequestId;
    private Long orderId;
    private Long orderItemId;
    private Long userId;
    private Long staffId;
    private String staffName;
    private Long productId;
    private String productName;
    private Integer productRating;
    private Integer staffRating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
