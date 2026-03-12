package com.trivine.llc.api.service;


import com.trivine.llc.api.entity.ReasonForApplying;
import com.trivine.llc.api.repository.ReasonForApplyingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReasonForApplyingService {
    private final ReasonForApplyingRepository repo;

    @Transactional(readOnly = true)
    @Cacheable("reasonForApplying")
    public List<ReasonForApplying> getAll() {
        return repo.findAllActive();
    }
}

