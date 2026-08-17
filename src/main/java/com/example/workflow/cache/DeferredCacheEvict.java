package com.example.workflow.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeferredCacheEvict {
    String cacheName();

    boolean allEntries() default false;

    String key() default "";
}
