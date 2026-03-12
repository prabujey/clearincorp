package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.response.AuditClickResponseDto;
import com.trivine.llc.api.entity.AuditClick;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditClickMapper {

    @Mapping(target = "businessId", source = "business.businessId")
    @Mapping(target = "ownerId",    source = "owner.ownerId")
    @Mapping(target = "deduped",    ignore = true)
    AuditClickResponseDto toDto(AuditClick entity);
}