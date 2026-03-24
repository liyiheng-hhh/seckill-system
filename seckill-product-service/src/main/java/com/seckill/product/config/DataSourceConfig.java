package com.seckill.product.config;

import com.seckill.product.datasource.DataSourceType;
import com.seckill.product.datasource.DynamicRoutingDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataSource dynamicDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        DynamicRoutingDataSource dynamic = new DynamicRoutingDataSource();
        dynamic.setDefaultTargetDataSource(masterDataSource);

        Map<Object, Object> targets = new HashMap<>();
        targets.put(DataSourceType.MASTER, masterDataSource);
        targets.put(DataSourceType.SLAVE, slaveDataSource);
        dynamic.setTargetDataSources(targets);
        dynamic.afterPropertiesSet();
        return dynamic;
    }
}
