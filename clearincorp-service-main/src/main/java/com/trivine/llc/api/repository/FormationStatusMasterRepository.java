package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.FormationStatusMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface FormationStatusMasterRepository extends JpaRepository<FormationStatusMaster, Long> {
    @Query("SELECT fsm FROM FormationStatusMaster fsm WHERE fsm.formationStatusId = :statusId")
    Optional<FormationStatusMaster> findById(@Param("statusId") Long statusId);
    Optional<FormationStatusMaster> findByFormationStatusName(String formationStatusName);
}