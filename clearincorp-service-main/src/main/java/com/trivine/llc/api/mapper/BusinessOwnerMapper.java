package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.BusinessOwnerDto;
import com.trivine.llc.api.entity.BusinessOwner;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BusinessOwnerMapper {
    BusinessOwnerDto toDto(BusinessOwner entity);
    BusinessOwner toEntity(BusinessOwnerDto dto);
}