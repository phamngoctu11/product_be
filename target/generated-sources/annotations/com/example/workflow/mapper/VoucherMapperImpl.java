package com.example.workflow.mapper;

import com.example.workflow.dto.UserVoucherDTO;
import com.example.workflow.dto.VoucherTemplateDTO;
import com.example.workflow.entity.UserVoucher;
import com.example.workflow.entity.VoucherTemplate;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-17T08:44:38+0700",
    comments = "version: 1.6.0, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class VoucherMapperImpl implements VoucherMapper {

    @Override
    public VoucherTemplateDTO toTemplateDto(VoucherTemplate entity) {
        if ( entity == null ) {
            return null;
        }

        VoucherTemplateDTO voucherTemplateDTO = new VoucherTemplateDTO();

        voucherTemplateDTO.setId( entity.getId() );
        voucherTemplateDTO.setCode( entity.getCode() );
        voucherTemplateDTO.setName( entity.getName() );
        voucherTemplateDTO.setDescription( entity.getDescription() );
        voucherTemplateDTO.setPointCost( entity.getPointCost() );
        voucherTemplateDTO.setMinOrderValue( entity.getMinOrderValue() );
        voucherTemplateDTO.setDiscountPercent( entity.getDiscountPercent() );
        voucherTemplateDTO.setMaxDiscountAmount( entity.getMaxDiscountAmount() );
        voucherTemplateDTO.setQuantity( entity.getQuantity() );
        voucherTemplateDTO.setActive( entity.isActive() );
        voucherTemplateDTO.setExpiryDate( entity.getExpiryDate() );

        return voucherTemplateDTO;
    }

    @Override
    public UserVoucherDTO toUserVoucherDto(UserVoucher entity) {
        if ( entity == null ) {
            return null;
        }

        UserVoucherDTO userVoucherDTO = new UserVoucherDTO();

        userVoucherDTO.setId( entity.getId() );
        userVoucherDTO.setTemplate( toTemplateDto( entity.getTemplate() ) );
        userVoucherDTO.setUsed( entity.isUsed() );
        userVoucherDTO.setRedeemDate( entity.getRedeemDate() );
        userVoucherDTO.setUsedDate( entity.getUsedDate() );

        return userVoucherDTO;
    }
}
