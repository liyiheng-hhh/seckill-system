package com.seckill.product.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ReadOnlyDataSourceAspect {

    @Around("@annotation(com.seckill.product.datasource.ReadOnlyDataSource)")
    public Object switchToSlave(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.useSlave();
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
        }
    }
}
