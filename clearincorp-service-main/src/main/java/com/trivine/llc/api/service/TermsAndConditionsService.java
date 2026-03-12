package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.TermsAndConditionsDto;
import com.trivine.llc.api.entity.TermsAndConditions;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.repository.TermsAndConditionsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TermsAndConditionsService {

    @Autowired
    private TermsAndConditionsRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(value = "terms", key = "#formTypeStr")
    public TermsAndConditionsDto getTermsByFormType(String formTypeStr) {
        log.info("Fetching terms for formType: {}", formTypeStr);

        TermsAndConditions terms = repository.findByFormType(formTypeStr)
                .orElseThrow(() -> new ResourceNotFoundException("Terms not found for formType: " + formTypeStr));

        return new TermsAndConditionsDto(terms.getFormType(), terms.getContent());
    }

}

