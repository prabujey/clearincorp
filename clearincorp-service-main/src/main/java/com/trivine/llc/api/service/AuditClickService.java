package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.AuditClickStatsDto;
import com.trivine.llc.api.dto.request.AuditClickRequestDto;
import com.trivine.llc.api.dto.response.AuditClickResponseDto;
import com.trivine.llc.api.entity.AuditClick;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessOwner;
import com.trivine.llc.api.mapper.AuditClickMapper;
import com.trivine.llc.api.repository.AuditClickRepository;
import com.trivine.llc.api.repository.BusinessOwnerRepository;
import com.trivine.llc.api.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditClickService {

    private final AuditClickRepository auditRepo;
    private final BusinessRepository businessRepo;
    private final BusinessOwnerRepository ownerRepo;
    private final AuditClickMapper mapper;


    @Transactional
    public AuditClickResponseDto track(AuditClickRequestDto in) {
        if (in.getBusinessId() == null || in.getBusinessId() <= 0)
            throw new IllegalArgumentException("businessId is required");

        Business business = businessRepo.findById(in.getBusinessId())
                .orElseThrow(() -> new IllegalArgumentException("Business not found: " + in.getBusinessId()));

        AuditClick saved;
        boolean deduped = false;

        if (in.getOwnerId() != null && in.getOwnerId() > 0) {
            boolean exists = auditRepo.existsByOwner_OwnerIdAndBusiness_BusinessId(in.getOwnerId(), in.getBusinessId());
            if (exists) {
                deduped = true;
                saved = AuditClick.builder()
                        .business(business)
                        .owner(BusinessOwner.builder().ownerId(in.getOwnerId()).build())
                        .build();
            } else {
                BusinessOwner owner = ownerRepo.findById(in.getOwnerId())
                        .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + in.getOwnerId()));

                saved = auditRepo.save(AuditClick.builder()
                        .business(business)
                        .owner(owner)
                        .build());

                businessRepo.incOwnerClickCounters(business.getBusinessId());
            }
        } else {

            String sessionId = (in.getSessionId() == null || in.getSessionId().isBlank())
                    ? UUID.randomUUID().toString()
                    : in.getSessionId();

            boolean exists = auditRepo.existsByBusiness_BusinessIdAndSessionId(in.getBusinessId(), sessionId);
            if (exists) {
                deduped = true;
                saved = AuditClick.builder()
                        .business(business)
                        .sessionId(sessionId)
                        .build();
            } else {
                saved = auditRepo.save(AuditClick.builder()
                        .business(business)
                        .sessionId(sessionId)
                        .build());

                businessRepo.incAnonClickCounters(business.getBusinessId());
            }
        }

        AuditClickResponseDto dto = mapper.toDto(saved);
        dto.setDeduped(deduped);
        return dto;
    }

    @Transactional(readOnly = true)
    public AuditClickStatsDto status(Long businessId) {
        long owners = auditRepo.countDistinctOwners(businessId);
        long anon   = auditRepo.countAnonymous(businessId);
        return AuditClickStatsDto.builder()
                .businessId(businessId)
                .uniqueOwners(owners)
                .anonymous(anon)
                .total(owners + anon)
                .build();
    }
}