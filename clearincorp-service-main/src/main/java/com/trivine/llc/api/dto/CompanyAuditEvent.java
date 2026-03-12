package com.trivine.llc.api.dto;




public record CompanyAuditEvent(Long companyId, Long loginUserId, String notes) {}
