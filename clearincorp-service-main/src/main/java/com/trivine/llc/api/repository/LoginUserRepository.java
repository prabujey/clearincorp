package com.trivine.llc.api.repository;

import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.entity.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface LoginUserRepository extends JpaRepository<LoginUser, Long> {
    Optional<LoginUser> findByEmail(String email);
    List<LoginUser> findByUserCompanyId(UserCompany userCompany);
    List<LoginUser> findByDeletedFalseAndRoleId_Id(Long roleId);
    List<LoginUser> findByDeletedFalseAndUserCompanyId_Id(Long companyId);
    boolean existsByEmail(String email);

    @Query("select u from LoginUser u where (:roleId is null or u.roleId.id = :roleId) and u.deleted = false and u.isActive = true")
    List<LoginUser> findActiveByRoleId(@Param("roleId") Long roleId);

    /**
     * Finds a user by combined first and last name (case-insensitive, ignoring whitespace).
     * Used for @mention resolution in forum posts.
     */
    @Query("SELECT u FROM LoginUser u WHERE LOWER(REPLACE(CONCAT(u.firstName, u.lastName), ' ', '')) = LOWER(:combinedName)")
    Optional<LoginUser> findByCombinedName(@Param("combinedName") String combinedName);
}

