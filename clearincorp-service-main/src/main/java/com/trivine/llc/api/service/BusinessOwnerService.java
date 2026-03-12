// src/main/java/com/trivine/llc/api/service/BusinessOwnerService.java
package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.BusinessOwnerDto;
import com.trivine.llc.api.dto.response.BusinessOwnerResponseDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessOwner;
import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.BusinessMapper;
import com.trivine.llc.api.mapper.BusinessOwnerMapper;
import com.trivine.llc.api.repository.BusinessOwnerRepository;
import com.trivine.llc.api.repository.BusinessRepository;
import com.trivine.llc.api.repository.LoginUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;


import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class BusinessOwnerService {

    private final BusinessOwnerRepository repo;
    private final BusinessOwnerMapper mapper;
    private final LoginUserRepository loginUserRepo;
    private final BusinessRepository businessRepo;
    private final BusinessMapper businessMapper;   // ✅ ADD
    private final BusinessOwnerMapper businessOwnerMapper;


    @Transactional(readOnly = true)
    public List<BusinessRegistrationDto> listAdminCreatedBusinesses() {

        List<Business> businesses = businessRepo.findAllAdminCreated();

        return businesses.stream()
                .map(b -> BusinessRegistrationDto.builder()
                        .owner(businessOwnerMapper.toDto(b.getOwner()))
                        .business(businessMapper.toDto(b))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessDto> getInProgressBusinessesByLoginUserId(Long loginUserId) {

        BusinessOwner owner = repo.findByLoginUserId(loginUserId)
                .orElseThrow(() -> new RuntimeException("Owner not found for loginUserId: " + loginUserId));

        return businessRepo.findInProgressByOwnerId(owner.getOwnerId())
                .stream().map(businessMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessDto> getRegisteredBusinessesByLoginUserId(Long loginUserId) {

        BusinessOwner owner = repo.findByLoginUserId(loginUserId)
                .orElseThrow(() -> new RuntimeException("Owner not found for loginUserId: " + loginUserId));

        return businessRepo.findRegisteredByOwnerId(owner.getOwnerId())
                .stream().map(businessMapper::toDto).toList();
    }


    @Transactional
    public BusinessOwnerDto create(BusinessOwnerDto dto) {
        BusinessOwner saved = repo.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public BusinessOwnerDto get(Long id) {
        BusinessOwner e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + id));
        return mapper.toDto(e);
    }

    @Transactional(readOnly = true)
    public List<BusinessOwnerDto> list() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public BusinessOwnerDto update(Long id, BusinessOwnerDto dto) {
        BusinessOwner e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + id));
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setPersonalEmail(dto.getPersonalEmail());
        e.setContactNumber(dto.getContactNumber());
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Owner not found: " + id);
        repo.deleteById(id);
    }


    @Transactional
    public BusinessOwner createRaw(BusinessOwner owner) {
        return repo.save(owner);
    }

    @Transactional(readOnly = true)
    public BusinessOwner findEntity(Long ownerId) {
        return repo.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerId));
    }

    @Transactional(readOnly = true)
    public Optional<BusinessOwnerResponseDto> getOwnerByPersonalEmailDto(String email) {
        return repo.findByPersonalEmail(email)
                .map(owner -> {
                    Long loginUserId = loginUserRepo.findByEmail(email)
                            .map(LoginUser::getLoginUserId)
                            .orElse(null); // OK if no login user exists

                    BusinessOwnerResponseDto dto = new BusinessOwnerResponseDto();
                    dto.setOwnerId(owner.getOwnerId());
                    dto.setFirstName(owner.getFirstName());
                    dto.setLastName(owner.getLastName());
                    dto.setPersonalEmail(owner.getPersonalEmail());
                    dto.setContactNumber(owner.getContactNumber());
                    dto.setLoginUserId(loginUserId);
                    return dto;
                });
    }

    @Transactional
    public BusinessOwner getOrCreateForLoginUser(Long loginUserId, BusinessOwnerDto ownerIn) {
        if (loginUserId == null || loginUserId <= 0) {
            throw new IllegalArgumentException("loginUserId is required");
        }

        // 1) If already mapped -> reuse
        Optional<BusinessOwner> byLogin = repo.findByLoginUserId(loginUserId);
        if (byLogin.isPresent()) {
            BusinessOwner existing = byLogin.get();
            // Optional: keep owner details updated if UI sends them
            if (ownerIn != null) {
                patchOwner(existing, ownerIn);
                existing = repo.save(existing);
            }
            return existing;
        }

        // 2) Canonical email from login_user table
        LoginUser user = loginUserRepo.findById(loginUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Login user not found: " + loginUserId));
        String email = user.getEmail();

        // 3) If old duplicates exist by email -> bind the oldest owner row to this loginUserId
        BusinessOwner old = repo.findFirstByPersonalEmailIgnoreCaseOrderByOwnerIdAsc(email).orElse(null);
        if (old != null && old.getLoginUserId() == null) {
            old.setLoginUserId(loginUserId);
            if (ownerIn != null) patchOwner(old, ownerIn);

            try {
                return repo.save(old);
            } catch (DataIntegrityViolationException ex) {
                // someone already created/bound it concurrently
                return repo.findByLoginUserId(loginUserId).orElseThrow(() -> ex);
            }
        }

        // 4) Create new owner (first time)
        if (ownerIn == null) {
            throw new IllegalArgumentException("owner details are required for first-time registration");
        }

        BusinessOwner created = mapper.toEntity(ownerIn);
        created.setOwnerId(null);           // force INSERT
        created.setLoginUserId(loginUserId);
        created.setPersonalEmail(email);    // safest: always use login_user email

        try {
            return repo.save(created);
        } catch (DataIntegrityViolationException ex) {
            return repo.findByLoginUserId(loginUserId).orElseThrow(() -> ex);
        }
    }

    private void patchOwner(BusinessOwner target, BusinessOwnerDto src) {
        if (src.getFirstName() != null) target.setFirstName(src.getFirstName());
        if (src.getLastName() != null) target.setLastName(src.getLastName());
        if (src.getContactNumber() != null) target.setContactNumber(src.getContactNumber());
        // DON'T change email from UI if you want it strictly tied to login_user email
    }

    @Transactional
    public void rejectBusiness(Long businessId) {
        businessRepo.markRejectedAndMaybeClearAdmin(businessId);
        // Always 200, even if nothing changed / id not found (as you requested)
    }




}
