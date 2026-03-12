package com.trivine.llc.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "myapp")
public class MyAppProperties {
    private Long defaultVendor;
    private Long adminCompany;

    public Long getDefaultVendor() {
        return defaultVendor;
    }
    public void setDefaultVendor(Long defaultVendor) {
        this.defaultVendor = defaultVendor;
    }

    public Long getAdminCompany() {
        return adminCompany;
    }
    public void setAdminCompany(Long adminCompany) {
        this.adminCompany = adminCompany;
    }
}

