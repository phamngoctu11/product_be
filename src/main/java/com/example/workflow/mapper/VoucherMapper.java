package com.example.workflow.mapper;

import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    VoucherTemplateDTO toTemplateDto(VoucherTemplate entity);
    UserVoucherDTO toUserVoucherDto(UserVoucher entity);
}