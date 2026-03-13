package com.trivine.llc.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Custom DataSource configuration.
 * Only activates when app.dataSource.jdbcUrl is explicitly configured.
 * Otherwise, Spring Boot's auto-configuration from spring.datasource.* is used.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.dataSource.jdbcUrl")
public class DbConfig {
    private final AppProperties appProperties;

    public DbConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public DataSource hikariDataSource() {
        log.info("Using custom DataSource Configuration from app.dataSource properties");
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
