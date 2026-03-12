package com.trivine.llc.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DbConfig {
    private final AppProperties appProperties;

    public DbConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public DataSource hikariDataSource() {
        log.info("Datasource Configuration: ");
        HikariConfig hikariConfig = new HikariConfig();
        AppProperties.DataSource dataSource = appProperties.getDataSource();
        hikariConfig.setJdbcUrl(dataSource.getJdbcUrl());
        hikariConfig.setUsername(dataSource.getUserName());
        hikariConfig.setPassword(dataSource.getPassword());
        hikariConfig.setMaximumPoolSize(dataSource.getPoolSize());
        hikariConfig.setMinimumIdle(dataSource.getIdleTime());
        hikariConfig.setMaxLifetime(dataSource.getMaxLifeTime());
        hikariConfig.setConnectionTimeout(dataSource.getConnectionTimeout());
        hikariConfig.setPoolName(dataSource.getPoolName());
        return new HikariDataSource(hikariConfig);
    }
}
