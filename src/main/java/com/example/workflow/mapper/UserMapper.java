package com.example.workflow.mapper;
import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
@Mapper(config = CentralMapperConfig.class, uses = CartMapper.class)
public interface UserMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "firstname", target = "firstname")
    @Mapping(source = "lastname", target = "lastname")
    @Mapping(source = "email", target = "email")
    UserCreDTO toKeycloakCreateRequest(UserCreDTO source);
    @Mapping(source="cart",target="cart")
    @Mapping(source="birth",target = "birth")
    @Mapping(source="phone",target="phone")
    UserResDTO toResponse(User user);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cart",ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "reputation", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "avatarUrl",source = "avatarUrl")
    void updateUser(@MappingTarget User user, UserCreDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "reputation", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    void updateProfile(@MappingTarget User user, UserProfileUpdateDTO request);
}
