package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.BusinessServiceDto;
import com.trivine.llc.api.entity.BusinessService;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.BusinessServiceMapper;
import com.trivine.llc.api.repository.BusinessServiceRepository;
import com.trivine.llc.api.repository.projection.ServiceIdName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class BusinessCatalogService {

    private final BusinessServiceRepository repo;
    private final BusinessServiceMapper mapper;

    @Transactional
    public BusinessServiceDto create(BusinessServiceDto dto) {
        BusinessService saved = repo.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public BusinessServiceDto get(Long id) {
        BusinessService e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
        return mapper.toDto(e);
    }

    @Transactional(readOnly = true)
    public List<ServiceIdName> listIdName() {
        return repo.findAllByOrderByServiceNameAsc();
    }

    @Transactional
    public BusinessServiceDto update(Long id, BusinessServiceDto dto) {
        BusinessService e = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
        e.setServiceName(dto.getServiceName());
        e.setDescription(dto.getDescription());
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Service not found: " + id);
        repo.deleteById(id);
    }
}
