package com.dev.lib.web;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
public class SlowRequestAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object monitSlowRequest(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long costMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (costMs > 500) {
                log.warn("slow request signature={} costMs={}", joinPoint.getSignature(), costMs);
            }
        }
    }

}
