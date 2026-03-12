package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.UserCompanyDto;
import com.trivine.llc.api.entity.UserCompany;
import com.trivine.llc.api.exception.ResourceNotFoundException;
import com.trivine.llc.api.mapper.UserCompanyMapper;
import com.trivine.llc.api.repository.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCompanyService {

    private final UserCompanyMapper userCompanyMapper;
    private final UserCompanyRepository userCompanyRepository;


    public List<UserCompanyDto> findAll() {
        log.info("Fetching all user companies...");
        List<UserCompany> entities = userCompanyRepository.findAll();
        return userCompanyMapper.toDtoList(entities);
    }

    public UserCompanyDto findById(Long id) {
        log.info("Fetching user company by ID: {}", id);
        return userCompanyRepository.findById(id)
                .map(userCompanyMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserCompany not found with ID: " + id));
    }

    public UserCompanyDto save(UserCompanyDto dto) {
        log.info("Saving user company...");
        UserCompany entity = userCompanyMapper.toEntity(dto);
        UserCompany saved = userCompanyRepository.save(entity);
        log.info("User company saved with ID: {}", saved.getId());
        return userCompanyMapper.toDto(saved);
    }

    public void deleteById(Long id) {
        log.info("Deleting user company with ID: {}", id);

        if (!userCompanyRepository.existsById(id)) {
            throw new ResourceNotFoundException("UserCompany not found with ID: " + id);
        }

        userCompanyRepository.deleteById(id);
        log.info("User company deleted with ID: {}", id);
    }

}
