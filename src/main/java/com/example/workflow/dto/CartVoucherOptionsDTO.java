package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartVoucherOptionsDTO {
    private double subtotal;
    private int currentReputation;
    private int redeemableReputation;
    private VoucherCartOptionDTO bestWalletVoucher;
    private VoucherCartOptionDTO bestRedeemableVoucher;
    private List<VoucherCartOptionDTO> walletVouchers;
    private List<VoucherCartOptionDTO> redeemableVouchers;
}
