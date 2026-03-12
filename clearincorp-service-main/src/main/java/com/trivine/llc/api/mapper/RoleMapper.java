package com.trivine.llc.api.mapper;

import com.trivine.llc.api.entity.Role;
import com.trivine.llc.api.dto.RoleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(Role entity);
    Role   toEntity(RoleDto dto);
}

