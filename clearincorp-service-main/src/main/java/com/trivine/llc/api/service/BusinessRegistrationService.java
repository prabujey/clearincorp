package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessAdminPageDto;
import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.BusinessOwnerDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.dto.response.PagedResponse;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessOwner;
import com.trivine.llc.api.entity.BusinessService;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.BusinessRegistrationMapper;
import com.trivine.llc.api.repository.BusinessRepository;
import com.trivine.llc.api.repository.BusinessServiceRepository;
import com.trivine.llc.api.repository.CompanyRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessRegistrationService {

    private final BusinessOwnerService ownerService;
    private final BusinessDirectoryService businessService;
    private final BusinessServiceRepository serviceRepo;
    private final BusinessRepository businessRepository;
    private final BusinessRegistrationMapper mapper;
    private final CompanyRepository companyRepository;


    @Transactional(readOnly = true)
    public List<BusinessRegistrationDto> listBusinessesByOwnerId(Long ownerId) {
        return businessRepository.findByOwner_OwnerId(ownerId)
                .stream()
                .map(mapper::toRegistrationDto)
                .toList();
    }

    @Transactional
    public BusinessRegistrationDto register(BusinessRegistrationDto in, Long loginUserId) {
        if (in == null) throw new IllegalArgumentException("Body is required");
        final BusinessOwnerDto ownerIn = in.getOwner();
        final BusinessDto      bisIn   = in.getBusiness();
        if (bisIn == null) throw new IllegalArgumentException("business is required");

        final String createdBy = resolveCreatedBy(loginUserId);

//        BusinessOwner owner;
//        if (ownerIn != null && ownerIn.getOwnerId() != null && ownerIn.getOwnerId() > 0) {
//            owner = ownerService.findEntity(ownerIn.getOwnerId());
//        } else {
//            if (ownerIn == null) throw new IllegalArgumentException("owner details are required when ownerId is not provided");
//            owner = mapper.toOwnerEntity(ownerIn);
//            owner = ownerService.createRaw(owner);
//        }
        BusinessOwner owner;

// ✅ If loginUserId is present -> always use "one owner per login user"
        if (loginUserId != null && loginUserId > 0) {
            owner = ownerService.getOrCreateForLoginUser(loginUserId, ownerIn);

            // Security: if UI sends some other ownerId, reject
            if (ownerIn != null && ownerIn.getOwnerId() != null
                    && !ownerIn.getOwnerId().equals(owner.getOwnerId())) {
                throw new IllegalArgumentException("ownerId does not belong to this login user");
            }

        } else {
            // Old behavior (only if you still want to allow register without loginUserId)
            if (ownerIn != null && ownerIn.getOwnerId() != null && ownerIn.getOwnerId() > 0) {
                owner = ownerService.findEntity(ownerIn.getOwnerId());
            } else {
                if (ownerIn == null) throw new IllegalArgumentException("owner details are required when ownerId is not provided");
                owner = mapper.toOwnerEntity(ownerIn);
                owner = ownerService.createRaw(owner);
            }
        }


        Long serviceId = bisIn.getServiceId();
        if (serviceId == null) throw new IllegalArgumentException("business.serviceId is required");
        BusinessService svc = serviceRepo.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("BusinessService not found: id=" + serviceId));

        Business business = mapper.toBusinessEntity(bisIn);
        business.setOwner(owner);
        business.setService(svc);
        business.setCreatedBy(createdBy);

        Business saved = businessService.createRaw(business);


        // ✅ loginUserId comes via owner mapping (business -> owner -> login_user_id)
        Long effectiveLoginUserId = owner.getLoginUserId();

// ✅ match: business.business_name  <-> company.company_name
        String nameToMatch = saved.getBusinessName(); // or bisIn.getBusinessName()

//        if (effectiveLoginUserId != null && StringUtils.hasText(nameToMatch)) {
//            companyRepository.markCompanyNewFlag(effectiveLoginUserId, nameToMatch.trim());
//        }

        return mapper.toRegistrationDto(saved);
    }

    @Transactional
    public void adminToggleVerify(Long businessId, Long loginUserId) {
        if (loginUserId == null || loginUserId != 1L) {  // your requested style
            throw new RuntimeException("Only admin can verify/unverify");
        }
        // try to verify
        int updated = businessRepository.markVerified(businessId, "admin");
        if (updated == 0) {
            // was already verified -> unverify
            businessRepository.unverify(businessId);
        }
    }

    private String resolveCreatedBy(Long loginUserId) {
        if (loginUserId == null) return "self";
        return (loginUserId == 1L) ? "admin" : "user";
    }
    @Transactional(readOnly = true)
    public List<BusinessRegistrationDto> getBusinessesByServiceId(Long serviceId) {
        return businessRepository.findByService_ServiceId(serviceId)
                .stream()
                .map(mapper::toRegistrationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<BusinessRegistrationDto> getAdminBusinesses(Long serviceId, String zipCode, Pageable pageable) {
        Specification<Business> spec = Specification.where(hasUpdatedByIgnoreCase());
        if (serviceId != null) spec = spec.and(hasServiceId(serviceId));
        if (StringUtils.hasText(zipCode)) spec = spec.and(hasZipCode(zipCode.trim()));
        return businessRepository.findAdminBusinesses(serviceId, zipCode, pageable)
                .map(mapper::toRegistrationDto);
    }
    private static Specification<Business> hasUpdatedByIgnoreCase() {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("updatedBy")), "admin".toLowerCase());
    }

    private static Specification<Business> hasServiceId(Long serviceId) {
        return (root, query, cb) ->
                cb.equal(root.join("service", JoinType.INNER).get("serviceId"), serviceId);
    }

    private static Specification<Business> hasZipCode(String zip) {
        return (root, query, cb) ->
                cb.equal(root.get("zipCode"), zip);
    }

    @Transactional(readOnly = true)
    public BusinessAdminPageDto getAdminBusinessesWithCounts(Long serviceId, String zipCode, Pageable pageable) {
        var page = businessRepository.findByAll(serviceId, zipCode, pageable)
                .map(mapper::toRegistrationDto);

        var counts = businessRepository.countVerifyBuckets(serviceId, zipCode);

        return BusinessAdminPageDto.builder()
                .page(PagedResponse.from(page))
                .verifiedCount(counts.getVerifiedCount())
                .unverifiedCount(counts.getUnverifiedCount())
                .totalCount(counts.getTotalCount())
                .build();
    }


    @Transactional(readOnly = true)
    public Page<BusinessRegistrationDto> list(Long serviceId, String zipCode, Pageable pageable) {
        Page<Business> page;

        if (serviceId != null && zipCode != null) {
            page = businessRepository.findByService_ServiceIdAndZipCode(serviceId, zipCode, pageable);
        } else if (serviceId != null) {
            page = businessRepository.findByService_ServiceId(serviceId, pageable);
        } else if (zipCode != null) {
            page = businessRepository.findByZipCode(zipCode, pageable);
        } else {
            page = businessRepository.findAll(pageable);
        }

        return page.map(mapper::toRegistrationDto);
    }

}