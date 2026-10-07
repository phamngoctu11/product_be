package com.example.workflow.mapper;

import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.entity.CustomRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CustomRequestMapper {
    @Mapping(target = "editable", expression = "java(request != null && request.isEditable())")
    CustomRequestDTO toDto(CustomRequest request);

    List<CustomRequestDTO> toDtoList(List<CustomRequest> requests);
}
