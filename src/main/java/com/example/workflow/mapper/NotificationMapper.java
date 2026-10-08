package com.example.workflow.mapper;

import com.example.workflow.dto.NotificationDTO;
import com.example.workflow.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(config = CentralMapperConfig.class)
public interface NotificationMapper {
    @Mapping(source = "read", target = "read")
    @Mapping(target = "createdAt", expression = "java(toInstant(notification.getCreatedAt()))")
    NotificationDTO toDto(Notification notification, boolean read);

    default Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant();
    }
}
