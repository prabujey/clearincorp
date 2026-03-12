package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.BusinessServiceDto;
import com.trivine.llc.api.entity.BusinessService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BusinessServiceMapper {
    BusinessServiceDto toDto(BusinessService entity);
    BusinessService toEntity(BusinessServiceDto dto);
}


