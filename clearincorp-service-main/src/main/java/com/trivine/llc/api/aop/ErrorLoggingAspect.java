package com.trivine.llc.api.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class ErrorLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingAspect.class);

    // Pointcut for all controller and service methods
    @Around("execution(* com.trivine.llc.api..Controller.(..)) || execution(* com.trivine.llc.api..Service.(..))")
    public Object logErrors(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Exception ex) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String method = signature.getDeclaringTypeName() + "." + signature.getName();
            log.error("Exception in [{}] with args {}: {}", method, Arrays.toString(joinPoint.getArgs()), ex.getMessage(), ex);
            throw ex;
        }
    }
}
