package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.FilingFailureCategoryDTO;
import com.trivine.llc.api.entity.FilingFailureCategory;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.FilingFailureCategoryMapper;
import com.trivine.llc.api.repository.FilingFailureCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilingFailureCategoryService {

    private final FilingFailureCategoryRepository repository;
    private final FilingFailureCategoryMapper filingFailureCategoryMapper;

//    public List<FilingFailureCategoryDTO> getAllFilingFailureCategories() {
//        log.info("Fetching all filing failure categories");
//        return repository.findAll().stream()
//                .map(filingFailureCategoryMapper::toDTO)
//                .collect(Collectors.toList());
//    }
    @Transactional(readOnly = true)
    @Cacheable("filingFailureCategories")
    public List<FilingFailureCategoryDTO> getAllFilingFailureCategories() {
        log.info("Fetching all filing failure categories");
        return repository.findAll().stream()
                .map(filingFailureCategoryMapper::toDTO)
                .collect(Collectors.toList());
    }

    public FilingFailureCategoryDTO getFilingFailureCategoryById(int id) {
        log.info("Fetching filing failure category by ID: {}", id);
        FilingFailureCategory entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found"));
        return filingFailureCategoryMapper.toDTO(entity);
    }

    public FilingFailureCategoryDTO saveFilingFailureCategory(FilingFailureCategoryDTO dto) {
        log.info("Saving filing failure category");
        FilingFailureCategory entity = filingFailureCategoryMapper.toEntity(dto);
        FilingFailureCategory savedEntity = repository.save(entity);
        return filingFailureCategoryMapper.toDTO(savedEntity);
    }

    public void deleteFilingFailureCategory(int id) {
        log.info("Deleting filing failure category with ID: {}", id);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Category with ID " + id + " does not exist");
        }

        repository.deleteById(id);
    }

}


