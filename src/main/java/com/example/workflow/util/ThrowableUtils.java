package com.example.workflow.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ThrowableUtils {

    public String rootMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
    }

    public String typedRootMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
}
