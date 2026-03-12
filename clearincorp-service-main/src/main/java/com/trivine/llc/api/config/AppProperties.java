package com.trivine.llc.api.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data //test
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final DataSource dataSource = new DataSource();
    @Data
    public static class DataSource {
        private String jdbcUrl;
        private String userName;
        private String password;
        private int poolSize;
        private int idleTime;
        private long maxLifeTime;
        private int connectionTimeout;
        private String poolName;
    }
}
