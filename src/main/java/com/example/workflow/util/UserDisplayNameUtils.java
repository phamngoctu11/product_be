package com.example.workflow.util;

import com.example.workflow.entity.User;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

@UtilityClass
public class UserDisplayNameUtils {
    public String fullName(User user) {
        return user == null ? null : TextNormalizer.fullName(user.getLastname(), user.getFirstname());
    }

    public String displayName(User user) {
        if (user == null) {
            return null;
        }
        String fullName = fullName(user);
        return StringUtils.hasText(fullName) ? fullName : user.getUsername();
    }
}
