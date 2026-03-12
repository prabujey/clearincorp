package com.trivine.llc.api.service;

import com.trivine.llc.api.dto.RoleDto;
import com.trivine.llc.api.entity.Role;
import com.trivine.llc.api.mapper.RoleMapper;
import com.trivine.llc.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repo;
    private final RoleMapper mapper;

    public RoleDto create(RoleDto dto) {
        Role saved = repo.save(mapper.toEntity(dto));
        log.info("Role created: {}", saved.getName());
        return mapper.toDto(saved);
    }

    public RoleDto getById(Long id) {
        return repo.findById(id)
                .map(role -> {
                    log.info("Fetched role by ID: {}", id);
                    return mapper.toDto(role);
                })
                .orElseThrow(() -> {
                    log.warn("Role not found with ID: {}", id);
                    return new RuntimeException("Role not found: " + id);
                });
    }


    public List<RoleDto> getAll() {
        List<RoleDto> roles = repo.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        log.info("Fetched all roles. Count: {}", roles.size());
        return roles;
    }


    public RoleDto update(Long id, RoleDto dto) {
        Role existing = repo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Role not found for update, ID: {}", id);
                    return new RuntimeException("Role not found: " + id);
                });

        existing.setName(dto.getName());
        existing.setAddBy(dto.getAddBy());
        existing.setLastModBy(dto.getLastModBy());
        existing.setIsDeleted(dto.getIsDeleted());

        Role updated = repo.save(existing);
        log.info("Updated role ID: {}", id);
        return mapper.toDto(updated);
    }


    public void delete(Long id) {
        Optional<Role> roleOpt = repo.findById(id);
        if (roleOpt.isPresent()) {
            Role role = roleOpt.get();
            role.setIsDeleted(true);
            repo.save(role);
            log.info("Soft-deleted role ID: {}", id);
        } else {
            log.warn("Role not found for deletion, ID: {}", id);
        }
    }

}
