package com.example.workflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConsultationReviewRequest {
    @Min(1)
    @Max(5)
    private int productRating;

    @Min(1)
    @Max(5)
    private int staffRating;

    @Size(max = 1000)
    private String comment;
}
