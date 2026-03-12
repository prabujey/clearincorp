package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessOwner;
import com.trivine.llc.api.entity.BusinessService;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.BusinessMapper;
import com.trivine.llc.api.mapper.BusinessRegistrationMapper;
import com.trivine.llc.api.repository.BusinessOwnerRepository;
import com.trivine.llc.api.repository.BusinessRepository;
import com.trivine.llc.api.repository.BusinessServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class BusinessDirectoryService {

    private final BusinessRepository repo;
    private final BusinessOwnerRepository ownerRepo;
    private final BusinessServiceRepository serviceRepo;
    private final BusinessMapper mapper;
    private final BusinessRegistrationMapper registrationMapper;

    @Transactional
    public BusinessDto create(BusinessDto dto) {
        Business entity = mapper.toEntity(dto);
        entity.setOwner(findOwner(dto.getOwnerId()));
        entity.setService(findService(dto.getServiceId()));
        copyUpdatableFields(dto, entity);
        return mapper.toDto(repo.save(entity));
    }

    @Transactional(readOnly = true)
    public BusinessRegistrationDto get(Long id) {
        Business e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
        return registrationMapper.toRegistrationDto(e);
    }


    @Transactional
    public BusinessDto update(Long id, BusinessDto dto) {
        Business e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));

        if (dto.getOwnerId() != null) e.setOwner(findOwner(dto.getOwnerId()));
        if (dto.getServiceId() != null) e.setService(findService(dto.getServiceId()));
        copyUpdatableFields(dto, e);

        return mapper.toDto(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Business not found: " + id);
        repo.deleteById(id);
    }

    /* helpers */
    private BusinessOwner findOwner(Long id) {
        return ownerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + id));
    }
    private BusinessService findService(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
    }
    private void copyUpdatableFields(BusinessDto s, Business t) {
        t.setBusinessName(s.getBusinessName());
        t.setServiceDescription(s.getServiceDescription());
        t.setYearsInBusiness(s.getYearsInBusiness());
        t.setZipCode(s.getZipCode());
        t.setBusinessAddress(s.getBusinessAddress());
        t.setWebsiteUrl(s.getWebsiteUrl());
        t.setBusinessLicense(s.getBusinessLicense());
        t.setBusinessEmail(s.getBusinessEmail());
        t.setCity(s.getCity());
        t.setState(s.getState());
        t.setCreatedBy(s.getCreatedBy());
        t.setUpdatedBy(s.getUpdatedBy());
    }

    @Transactional
    public Business createRaw(Business business) {
        return repo.save(business);
    }
}