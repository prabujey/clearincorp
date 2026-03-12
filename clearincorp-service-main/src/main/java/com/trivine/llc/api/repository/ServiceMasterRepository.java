package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.ServiceMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceMasterRepository extends JpaRepository<ServiceMaster, Long> {

    @Query(value = "SELECT * FROM service_master WHERE service_id = :serviceId", nativeQuery = true)
    Optional<ServiceMaster> findByServiceId(@Param("serviceId") Long serviceId);

    Optional<ServiceMaster> findByServiceName(String serviceName);



}