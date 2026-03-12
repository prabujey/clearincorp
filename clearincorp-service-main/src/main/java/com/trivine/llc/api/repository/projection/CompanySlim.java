package com.trivine.llc.api.repository.projection;


public interface CompanySlim {
    String getState();
    Long getCompanyId();
    String getCompanyName();
    Long getLoginUserId();
    Long getBusinessId();
    String getStatusName();
    String getBusinessUpdatedBy();
}

