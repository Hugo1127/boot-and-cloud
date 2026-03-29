package com.bootcloud.core.test;

import com.bootcloud.core.aop.Aspect;
import com.bootcloud.core.aop.Around;
import com.bootcloud.core.aop.Before;
import com.bootcloud.core.aop.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Before("com.bootcloud.core.test.UserService.*(..)")
    public void before(ProceedingJoinPoint joinPoint) {
        logger.info("Before advice: {}", joinPoint.getSignature());
    }

    @After("com.bootcloud.core.test.UserService.*(..)")
    public void after(ProceedingJoinPoint joinPoint) {
        logger.info("After advice: {}", joinPoint.getSignature());
    }

    @Around("com.bootcloud.core.test.OrderService.*(..)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        logger.info("Around before: {}", joinPoint.getSignature());
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        logger.info("Around after: {}, time: {}ms", joinPoint.getSignature(), (endTime - startTime));
        return result;
    }
}
