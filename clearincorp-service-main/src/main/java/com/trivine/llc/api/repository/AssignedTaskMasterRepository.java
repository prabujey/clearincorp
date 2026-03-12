package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.AssignedTaskMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AssignedTaskMasterRepository extends JpaRepository<AssignedTaskMaster, String>, JpaSpecificationExecutor<AssignedTaskMaster> {

}
