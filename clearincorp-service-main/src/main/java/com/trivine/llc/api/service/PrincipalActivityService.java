package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.PrincipalActivityDto;
import com.trivine.llc.api.repository.PrincipalActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PrincipalActivityService {

    private final PrincipalActivityRepository repository;

    @Transactional(readOnly = true)
    public List<PrincipalActivityDto> getAll() {
        return repository.findAllByOrderByValueAsc()
                .stream()
                .map(pa -> PrincipalActivityDto.builder()
                        .id(pa.getId())
                        .value(pa.getValue())
                        .subActivities(pa.getSubActivities())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable("principalActivity")
    public List<String> getAllValuesOnly() {
        return repository.findAllValuesOrderByValueAsc();
    }
    @Transactional(readOnly = true)
    @Cacheable(value = "subActivitiesByCategory", key = "#value")
    public List<String> getSubsByValue(String value) {
        return repository.findSubActivitiesByCategoryValue(value);
    }

    @Transactional(readOnly = true)
    public Optional<PrincipalActivityDto> getByValue(String value) {
        return repository.findByValueIgnoreCase(value)
                .map(pa -> PrincipalActivityDto.builder()
                        .id(pa.getId())
                        .value(pa.getValue())
                        .subActivities(pa.getSubActivities())
                        .build());
    }
}

