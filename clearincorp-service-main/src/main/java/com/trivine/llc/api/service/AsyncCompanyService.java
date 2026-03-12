package com.trivine.llc.api.service;

import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Async service for fetching company-related data in parallel.
 * Methods return CompletableFuture that will complete exceptionally on errors,
 * allowing callers to handle failures appropriately.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyPrincipalRepository companyPrincipalRepository;
    private final RegisteredAgentRepository registeredAgentRepository;
    private final CompanyServiceRepository companyServiceRepository;
    private final ManagerMemberRepository managerMemberRepository;
    private final MemberRepository memberRepository;
    private final FormationStatusRepository formationStatusRepository;
    private final EinDetailsRepository einDetailsRepository;

    @Async("taskExecutor")
    public CompletableFuture<Optional<Company>> getCompany(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<Company> result = companyRepository.findById(companyId);
            log.debug("Company query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch company with ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<CompanyPrincipal>> getPrincipal(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<CompanyPrincipal> result = companyPrincipalRepository.findByCompanyId(companyId);
            log.debug("CompanyPrincipal query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch principal for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<RegisteredAgent>> getAgentByCompanyId(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<RegisteredAgent> result = registeredAgentRepository.findByCompany_CompanyId(companyId);
            log.debug("RegisteredAgent query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch registered agent for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<List<CompanyServices>>> getCompanyServices(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<List<CompanyServices>> result = companyServiceRepository.findWithServiceMaster(companyId);
            log.debug("CompanyServices query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch company services for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<List<ManagerMember>>> getManagerMembers(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<List<ManagerMember>> result = managerMemberRepository.findByCompany_CompanyId(companyId);
            log.debug("ManagerMembers query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch manager members for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<List<Member>>> getMembers(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<List<Member>> result = memberRepository.findByCompany_CompanyId(companyId);
            log.debug("Member query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch members for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<FormationStatus>> getFormationStatus(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<FormationStatus> result = formationStatusRepository.findFirstByCompanyIdAndIsActiveTrue(companyId);
            log.debug("FormationStatus query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch formation status for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<Optional<EinDetails>> getEinDetails(Long companyId) {
        long start = System.currentTimeMillis();
        try {
            Optional<EinDetails> result = einDetailsRepository.findByCompany_CompanyId(companyId);
            log.debug("EinDetails query time: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to fetch EIN details for company ID {}: {}", companyId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
