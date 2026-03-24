package com.seckill.product.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType current = DataSourceContextHolder.getCurrent();
        return current == null ? DataSourceType.MASTER : current;
    }
}
