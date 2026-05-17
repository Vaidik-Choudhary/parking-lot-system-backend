package com.parkease.payment.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@annotation(com.parkease.payment.annotation.TrackExecutionTime)")
    public Object executionTimeLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object proceed = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;
        if (log.isInfoEnabled()) {
            log.info("{} method executed in {} ms", joinPoint.getSignature().toShortString(), executionTime);
        }
        return proceed;
    }
}
