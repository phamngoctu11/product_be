package com.example.workflow.util;

import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@UtilityClass
public class GuestSessionUtils {
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9._:-]{16,128}$");

    public String normalize(String guestSessionId) {
        if (!StringUtils.hasText(guestSessionId)) {
            throw invalid();
        }
        String normalized = guestSessionId.trim();
        if (!VALID_ID.matcher(normalized).matches()) {
            throw invalid();
        }
        return normalized;
    }

    private AppException invalid() {
        return new AppException(
                HttpStatus.BAD_REQUEST,
                ConstantErrorCode.BAD_REQUEST_DETAIL,
                "Guest session id is invalid."
        );
    }
}
