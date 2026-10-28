package com.example.workflow.service;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Pure catalog duration rule. The result is calculated independently for each OrderItem. */
@Service
public class CatalogDurationCalculator {
    public static final String RULE_VERSION = "CATALOG_V1";

    public int calculate(Double madeDay, int quantity) {
        if (madeDay == null || !Double.isFinite(madeDay) || madeDay < 2.0) {
            throw new AppException(
                    HttpStatus.CONFLICT,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Sản phẩm trong danh mục chưa được cấu hình thời gian sản xuất hợp lệ."
            );
        }
        if (quantity < 1) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ConstantErrorCode.BAD_REQUEST_DETAIL,
                    "Số lượng phải từ 1 trở lên.."
            );
        }

        double makeDays = quantity > 5
                ? madeDay + 1.0 + (madeDay - 1.0) * 4.0 + (madeDay - 1.5) * (quantity - 5.0)
                : madeDay + 1.0 + (madeDay - 1.0) * (quantity - 1.0);

        return Math.toIntExact((long) Math.ceil(makeDays) + (long) Math.ceil(makeDays * 0.1));
    }
}
