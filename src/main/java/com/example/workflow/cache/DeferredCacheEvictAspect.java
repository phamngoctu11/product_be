package com.example.workflow.cache;

import com.example.workflow.event.payload.CacheEvictionEntry;
import com.example.workflow.service.redis.DeferredCacheEvictionPublisher;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class DeferredCacheEvictAspect {
    private final DeferredCacheEvictionPublisher publisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(deferredCacheEvicts)")
    public Object publishCacheEvictionsAfterSuccessfulReturn(
            ProceedingJoinPoint joinPoint,
            DeferredCacheEvicts deferredCacheEvicts
    ) throws Throwable {
        Object result = joinPoint.proceed();

        List<CacheEvictionEntry> entries = buildEntries(joinPoint, deferredCacheEvicts, result);
        publisher.publishEventually(deferredCacheEvicts.reason(), entries);

        return result;
    }

    private List<CacheEvictionEntry> buildEntries(
            ProceedingJoinPoint joinPoint,
            DeferredCacheEvicts deferredCacheEvicts,
            Object result
    ) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        StandardEvaluationContext context = buildEvaluationContext(method, joinPoint.getArgs(), result);

        List<CacheEvictionEntry> entries = new ArrayList<>();
        for (DeferredCacheEvict evict : deferredCacheEvicts.value()) {
            if (evict.allEntries()) {
                entries.add(CacheEvictionEntry.allEntries(evict.cacheName()));
                continue;
            }
            if (!evict.key().isBlank()) {
                Object key = parser.parseExpression(evict.key()).getValue(context);
                entries.add(CacheEvictionEntry.key(evict.cacheName(), key));
            }
        }
        return entries;
    }

    private StandardEvaluationContext buildEvaluationContext(Method method, Object[] args, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("result", result);
        return context;
    }
}
