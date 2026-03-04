package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucherDTO {
    private Long id;
    private VoucherTemplateDTO template;
    private boolean isUsed;
    private LocalDateTime redeemDate;
    private LocalDateTime usedDate;
}