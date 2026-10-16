package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import com.example.workflow.nume.OrderType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderResponseDTO implements Serializable {
    private OrderType orderType;
    private Long customDraftId;
    private int addedItemCount;
    private int skippedItemCount;
    private List<ReorderItemDTO> addedItems;
    private List<ReorderItemDTO> skippedItems;
}
