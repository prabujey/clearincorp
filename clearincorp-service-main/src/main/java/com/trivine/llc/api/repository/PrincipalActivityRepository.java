package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.PrincipalActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PrincipalActivityRepository extends JpaRepository<PrincipalActivity, Long> {

    @Query(value = """
    SELECT jt.sub
    FROM principal_activity pa,
         JSON_TABLE(pa.sub_activities, '$[*]'
           COLUMNS(sub VARCHAR(150) PATH '$')) jt
    WHERE LOWER(pa.value) = LOWER(?1)
    ORDER BY jt.sub
    """, nativeQuery = true)
    List<String> findSubActivitiesByCategoryValue(String value);
    List<PrincipalActivity> findAllByOrderByValueAsc();
    Optional<PrincipalActivity> findByValueIgnoreCase(String value);
    @Query("select p.value from PrincipalActivity p order by p.value asc")
    List<String> findAllValuesOrderByValueAsc();
}


