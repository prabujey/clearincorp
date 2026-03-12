package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.StateFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StateFeeRepository extends JpaRepository<StateFee, Long> {
    Optional<StateFee> findByStateId(long state_id);
    Optional<StateFee> findByState(String state);
    Optional<StateFee> findByStateIgnoreCase(String state);       // "Texas" → row
    Optional<StateFee> findByStateKeyIgnoreCase(String stateKey);
    @Query("select s.state from StateFee s where s.active = true order by s.state")
    List<String> findAllActiveStateNames();
}
