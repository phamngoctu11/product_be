package com.example.workflow.mapper;

import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.dto.CreateVoucherTemplateRequest;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = CentralMapperConfig.class)
public interface VoucherMapper {
    VoucherTemplateDTO toTemplateDto(VoucherTemplate entity);
    UserVoucherDTO toUserVoucherDto(UserVoucher entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    VoucherTemplate toEntity(CreateVoucherTemplateRequest request);
}
