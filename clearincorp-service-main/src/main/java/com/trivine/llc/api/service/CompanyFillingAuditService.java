package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.response.CompanyDetailsDto;
import com.trivine.llc.api.entity.*;
import com.trivine.llc.api.repository.CompanyFillingAuditRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CompanyFillingAuditService {


    private final AsyncCompanyService asyncCompanyService;

    private final CompanyFillingAuditRepository companyFillingAuditRepository;

    @Transactional
    public void setJsonDetails(Long companyId, Long loginUserId,String notes) throws Exception {
        long start = System.currentTimeMillis();

        CompanyDetailsDto companyDetailsDto = new CompanyDetailsDto();

        CompletableFuture<Optional<Company>> companyFuture = asyncCompanyService.getCompany(companyId);
        CompletableFuture<Optional<CompanyPrincipal>> principalFuture = asyncCompanyService.getPrincipal(companyId);
        CompletableFuture<Optional<RegisteredAgent>> agentFuture = asyncCompanyService.getAgentByCompanyId(companyId);
        CompletableFuture<Optional<List<ManagerMember>>> managerFuture = asyncCompanyService.getManagerMembers(companyId);
        CompletableFuture<Optional<List<Member>>> memberFuture = asyncCompanyService.getMembers(companyId);

        CompletableFuture.allOf(companyFuture, principalFuture, agentFuture, managerFuture, memberFuture)
                .join();

        long end = System.currentTimeMillis();
        System.out.println("Fetched company details in: " + (end - start) + " ms");

        // Manually create the JSON object
        JSONObject companyDetailsJson = new JSONObject();

        // Set company details directly from the asynchronous result
        companyFuture.thenAccept(optionalCompany ->
                optionalCompany.ifPresent(company -> {
                    companyDetailsJson.put("companyId", company.getCompanyId());
                    companyDetailsJson.put("state", company.getState());
                    companyDetailsJson.put("formationDate", company.getCompanyEffectiveDate());
                    companyDetailsJson.put("companyRequestDto", new JSONObject()
                            .put("companyName", company.getCompanyName())
                            .put("llcsuffix", company.getSuffixMaster().getSuffix()));
                    companyDetailsJson.put("mailingAddress", new JSONObject()
                            .put("streetAddress1", company.getStreetAddress1())
                            .put("streetAddress2", company.getStreetAddress2())
                            .put("city", company.getCity())
                            .put("state", company.getState())
                            .put("zip", company.getZipCode()));
                })
        ).join();

        principalFuture.thenAccept(optionalPrincipal ->
                optionalPrincipal.ifPresent(principal -> {
                    // Add relevant fields from CompanyPrincipal
                    companyDetailsJson.put("companyPrimaryContact", new JSONObject()
                            .put("firstName", principal.getFirstName())
                            .put("lastName", principal.getLastName())
                            .put("email", principal.getEmail())
                            .put("phoneNumber", principal.getPhoneNumber()));
                })
        ).join();

        agentFuture.thenAccept(optionalAgent ->
                optionalAgent.ifPresent(agent -> {
                    // Add relevant fields from RegisteredAgent
                    companyDetailsJson.put("registeredAgent", new JSONObject()
                            .put("firstName", agent.getFirstName())
                            .put("lastName", agent.getLastName())
                            .put("address1", agent.getStreetAddress1())
                            .put("address2", agent.getStreetAddress2())
                            .put("city", agent.getCity())
                            .put("state", agent.getState())
                            .put("zip", agent.getZipCode()));
                })
        ).join();

        // Add all manager details to the JSON
        managerFuture.thenAccept(optionalManagers ->
                optionalManagers.ifPresent(managers -> {
                    JSONArray managersArray = new JSONArray();
                    for (ManagerMember manager : managers) {
                        JSONObject managerJson = new JSONObject();
                        managerJson.put("managerId", manager.getManagerId());
                        managerJson.put("firstName", manager.getFirstName());
                        managerJson.put("lastName", manager.getLastName());
                        managerJson.put("email", manager.getEmail());
                        managerJson.put("phoneNumber", manager.getPhoneNumber());
                        managerJson.put("streetAddress1", manager.getStreetAddress1());
                        managerJson.put("streetAddress2", manager.getStreetAddress2());
                        managerJson.put("city", manager.getCity());
                        managerJson.put("state", manager.getState());
                        managerJson.put("zipCode", manager.getZipCode());
                        managersArray.put(managerJson);
                    }
                    companyDetailsJson.put("managers", managersArray);
                })
        ).join();

        // Add all member details to the JSON
        memberFuture.thenAccept(optionalMembers ->
                optionalMembers.ifPresent(members -> {
                    JSONArray membersArray = new JSONArray();
                    for (Member member : members) {
                        JSONObject memberJson = new JSONObject();
                        memberJson.put("memberId", member.getMemberId());
                        memberJson.put("firstName", member.getFirstName());
                        memberJson.put("lastName", member.getLastName());
                        memberJson.put("email", member.getEmail());
                        memberJson.put("phone", member.getPhoneNumber());
                        memberJson.put("address1", member.getStreetAddress1());
                        memberJson.put("address2", member.getStreetAddress2());
                        memberJson.put("city", member.getCity());
                        memberJson.put("state", member.getState());
                        memberJson.put("zip", member.getZipCode());
                        memberJson.put("country", member.getCountry());
                        memberJson.put("ownership", member.getOwnership());
                        membersArray.put(memberJson);
                    }
                    companyDetailsJson.put("members", membersArray);
                })
        ).join();

        String jsonString = companyDetailsJson.toString();

        companyFillingAuditRepository.deactivateOldRecords(companyId);

        CompanyFillingAudit audit = new CompanyFillingAudit(companyId, loginUserId, jsonString);
        audit.setCreatedOn(LocalDateTime.now());
        audit.setIsActive(true);

        audit.setNotes(notes);

        companyFillingAuditRepository.save(audit);
    }
}
