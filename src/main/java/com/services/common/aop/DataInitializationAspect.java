package com.services.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Aspect
@Component
@Slf4j
public class DataInitializationAspect {

    @Around("execution(* com.services.common.application.DataInitializationService.setDataStorage(..))")
    public Object logDataInitialization(ProceedingJoinPoint joinPoint) throws Throwable {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        Instant startTime = Instant.now();
        
        log.info("🚀 Starting data initialization for {}", serviceName);
        
        try {
            Object result = joinPoint.proceed();
            
            Duration duration = Duration.between(startTime, Instant.now());
            log.info("✅ Successfully completed data initialization for {} in {} seconds", 
                    serviceName, duration.toSeconds());
                    
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("❌ Failed to initialize data for {} after {} seconds: {}", 
                    serviceName, duration.toSeconds(), e.getMessage());
            throw e;
        }
    }
    
    @Around("execution(* com.services.common.application.DataInitializationService.getFetchDataList(..))")
    public Object logDataFetching(ProceedingJoinPoint joinPoint) throws Throwable {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        Instant startTime = Instant.now();
        
        log.debug("📡 Starting data fetching for {}", serviceName);
        
        try {
            Object result = joinPoint.proceed();
            
            Duration duration = Duration.between(startTime, Instant.now());
            if (result instanceof java.util.List) {
                int itemCount = ((java.util.List<?>) result).size();
                log.info("📊 Successfully fetched {} items for {} in {} seconds", 
                        itemCount, serviceName, duration.toSeconds());
            }
                    
            return result;
            
        } catch (Exception e) {
            Duration duration = Duration.between(startTime, Instant.now());
            log.error("🚨 Failed to fetch data for {} after {} seconds: {}", 
                    serviceName, duration.toSeconds(), e.getMessage());
            throw e;
        }
    }
}