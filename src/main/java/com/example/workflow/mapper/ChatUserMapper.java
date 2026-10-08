package com.example.workflow.mapper;

import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.dto.ConsultationRequestDTO;
import com.example.workflow.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface ChatUserMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstname", target = "firstname")
    @Mapping(source = "lastname", target = "lastname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(target = "isActive", constant = "false")
    ChatUserDTO fromUser(User user);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(source = "id", target = "id")
    @Mapping(source = "firstname", target = "firstname")
    @Mapping(source = "lastname", target = "lastname")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    @Mapping(source = "isActive", target = "isActive")
    ChatUserDTO copyIdentity(ChatUserDTO source);

    default ChatUserDTO toStaffThread(ChatUserDTO customer, ConsultationRequestDTO request) {
        if (customer == null) {
            return null;
        }
        ChatUserDTO result = copyIdentity(customer);
        applyThread(result, request);
        result.setChatTitle(request.getCustomerName() + " - " + request.getProductName());
        return result;
    }

    default ChatUserDTO toCustomerThread(User customer, ConsultationRequestDTO request, boolean staffOnline) {
        ChatUserDTO result = fromUser(customer);
        applyThread(result, request);
        result.setIsActive(request.getAssignedStaffId() != null && staffOnline);
        String staffName = request.getAssignedStaffName() == null || request.getAssignedStaffName().isBlank()
                ? "Dang cho nhan vien"
                : request.getAssignedStaffName();
        result.setChatTitle(request.getProductName() + " - " + staffName);
        return result;
    }

    default void applyThread(ChatUserDTO target, ConsultationRequestDTO request) {
        target.setChatThreadId(request.getId());
        target.setConsultationRequestId(request.getId());
        target.setProductId(request.getProductId());
        target.setProductName(request.getProductName());
        target.setProductImageUrl(request.getProductImageUrl());
        target.setAssignedStaffId(request.getAssignedStaffId());
        target.setAssignedStaffName(request.getAssignedStaffName());
        target.setAssignedByManagerId(request.getAssignedByManagerId());
        target.setAssignedByManagerName(request.getAssignedByManagerName());
    }
}
