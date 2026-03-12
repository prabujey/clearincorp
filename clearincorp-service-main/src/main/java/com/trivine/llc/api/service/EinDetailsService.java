package com.trivine.llc.api.service;

import com.trivine.llc.api.crypto.EncryptResult;
import com.trivine.llc.api.crypto.PiiCryptoService;
import com.trivine.llc.api.dto.EinDetailsDto;
import com.trivine.llc.api.entity.EinDetails;
import com.trivine.llc.api.mapper.EinDetailsMapper;
import com.trivine.llc.api.repository.EinDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EinDetailsService {

    private final EinDetailsRepository repository;
    private final EinDetailsMapper mapper;
    private final PiiCryptoService piiCryptoService;

    public EinDetailsDto saveEinDetails(EinDetailsDto dto) throws Exception {
        // Check if record already exists for the company
        Optional<EinDetails> existingEin = repository.findByCompany_CompanyId(dto.getCompanyId());

        EinDetails entity;
        EncryptResult encryptResult=piiCryptoService.encrypt(dto.getSsnId());
        dto.setSsnIdCipherJson(encryptResult);
        if (existingEin.isPresent()) {
            // Update existing entity
            entity = existingEin.get();

            // Map updated fields from DTO → Entity (but keep the same einId)
            mapper.updateEntityFromDto(dto, entity);
        } else {
            // Create new entity
            entity = mapper.toEntity(dto);
        }
        EinDetails saved = repository.save(entity);
        saved.setSsnIdCipherJson(null);
        return mapper.toDto(saved);
    }



    public List<EinDetailsDto> getAllEinDetails() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public EinDetailsDto getEinDetailsById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("EIN Details not found"));
    }

    public void deleteEinDetails(Long id) {
        repository.deleteById(id);
    }
}