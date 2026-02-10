package com.example.workflow.mapper;
import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
@Mapper(componentModel = "spring",uses = {CartMapper.class})
public interface UserMapper {
    @Mapping(target = "role",source = "role")
    User toEntity(UserCreDTO request);
    @Mapping(source="cart",target="cart")
    UserResDTO toResponse(User user);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "cart",ignore = true)
    @Mapping(target = "role",source = "role")
    void updateUser(@MappingTarget User user, UserCreDTO request);
}