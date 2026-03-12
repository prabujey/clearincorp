package com.trivine.llc.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheCfg {
    @Bean
    public org.springframework.cache.CacheManager caffeineCacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager("entityCheck","states","suffixes","documentTypes","filingFailureCategories","principalActivity", "subActivitiesByCategory","reasonForApplying","terms");
        cm.setCaffeine(Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofHours(12)));
        return cm;
    }
}