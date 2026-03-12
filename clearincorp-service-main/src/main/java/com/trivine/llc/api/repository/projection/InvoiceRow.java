package com.trivine.llc.api.repository.projection;

import java.time.LocalDateTime;

public interface InvoiceRow {
    Long getPaymentId();
    Long getCompanyId();
    String getInvoiceId();
    String getStatus();
    java.time.LocalDateTime getPaymentDate();
    Double getPaymentAmount();

    String getCompanyName();
    String getSuffix();
    String getStreetAddress1();
    String getCompanyPhone1();
    String getState();
    String getBillToEmail();

    String getServiceName();              // svc.serviceName
    java.math.BigDecimal getServicePrice(); // cs.servicePrice
}


