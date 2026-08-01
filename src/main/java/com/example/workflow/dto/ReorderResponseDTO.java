package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderResponseDTO implements Serializable {
    private int addedItemCount;
    private int skippedItemCount;
    private List<ReorderItemDTO> addedItems;
    private List<ReorderItemDTO> skippedItems;
}
