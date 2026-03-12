package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.SuffixMasterDTO;
import com.trivine.llc.api.entity.SuffixMaster;
import com.trivine.llc.api.mapper.SuffixMasterMapper;
import com.trivine.llc.api.repository.SuffixMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuffixMasterService {

    private final SuffixMasterRepository suffixMasterRepository;

    public SuffixMasterDTO saveOrFetchSuffix(SuffixMasterDTO suffixMasterDTO) {
        log.info("Checking if suffix '{}' already exists", suffixMasterDTO.getSuffix());

        Optional<SuffixMaster> existingSuffix = suffixMasterRepository.findBySuffix(suffixMasterDTO.getSuffix());

        if (existingSuffix.isPresent()) {
            log.info("Suffix '{}' already exists. Returning existing record.", suffixMasterDTO.getSuffix());
            return SuffixMasterMapper.INSTANCE.toDTO(existingSuffix.get());
        }

        log.info("Suffix '{}' does not exist. Creating new entry.", suffixMasterDTO.getSuffix());

        SuffixMaster suffixMaster = SuffixMasterMapper.INSTANCE.toEntity(suffixMasterDTO);
        suffixMaster = suffixMasterRepository.save(suffixMaster);

        log.info("New suffix '{}' saved with ID {}", suffixMaster.getSuffix(), suffixMaster.getSuffixId());

        return SuffixMasterMapper.INSTANCE.toDTO(suffixMaster);
    }
}
