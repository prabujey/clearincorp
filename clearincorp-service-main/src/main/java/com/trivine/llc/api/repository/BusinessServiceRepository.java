package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.BusinessService;
import com.trivine.llc.api.repository.projection.ServiceIdName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessServiceRepository extends JpaRepository<BusinessService, Long> {
    List<ServiceIdName> findAllByOrderByServiceNameAsc();
    Optional<BusinessService> findByServiceName(String serviceName);
    Optional<BusinessService> findByServiceNameIgnoreCase(String serviceName);
}