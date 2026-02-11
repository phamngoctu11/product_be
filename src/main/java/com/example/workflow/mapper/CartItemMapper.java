package com.example.workflow.mapper;

import com.example.workflow.dto.CartItemDTO;
import com.example.workflow.entity.CartItem;
import org.mapstruct.Mapper;
import java.util.List;
@Mapper(componentModel = "spring")
public interface CartItemMapper {
    CartItemDTO toResponseDTO(CartItem entity);
    CartItem toEntity(CartItemDTO dto);
    List<CartItemDTO> toResponseDTOList(List<CartItem> entities);
}