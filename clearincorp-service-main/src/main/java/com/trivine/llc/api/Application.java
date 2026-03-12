package com.trivine.llc.api;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EntityScan(basePackages = "com.trivine.llc.api.entity")
public class Application {
    public static void main(String[] args) {
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        SpringApplication.run(Application.class);

    }
}
