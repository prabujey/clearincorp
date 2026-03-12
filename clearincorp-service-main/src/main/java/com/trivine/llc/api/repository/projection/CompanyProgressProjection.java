package com.trivine.llc.api.repository.projection;

import com.trivine.llc.api.entity.*;

public interface CompanyProgressProjection {
    Company getCompany();
    CompanyPrincipal getCompanyPrincipal();
    RegisteredAgent getRegisteredAgent();
    FormationStatus getFormationStatus();
    EinDetails getEinDetails();
}


