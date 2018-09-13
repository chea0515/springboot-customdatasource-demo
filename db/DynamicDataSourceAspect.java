package com.cc.bms.base.config.db;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(-1)
@Component
public class DynamicDataSourceAspect {

    @Before("@annotation(targetDataSource)")
    public void changeBefore(JoinPoint point, TargetDataSource targetDataSource) {
        String dataSourceId = targetDataSource.value();
        if (!DynamicDataSourceContextHolder.contains(dataSourceId)) {
            throw new RuntimeException("not found dataSource:" + dataSourceId);
        }
        DynamicDataSourceContextHolder.set(dataSourceId);
    }

    @After("@annotation(targetDataSource)")
    public void changeAfter(JoinPoint point, TargetDataSource targetDataSource) {
        DynamicDataSourceContextHolder.remove();
    }
}
