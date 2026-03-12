package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.entity.Business;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BusinessMapper {

    @Mapping(target = "ownerId", source = "owner.ownerId")
    @Mapping(target = "serviceId", source = "service.serviceId")
    @Mapping(target = "serviceName", source = "service.serviceName")
    BusinessDto toDto(Business entity);

    // Relations are set in the Service layer (we ignore here)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "service", ignore = true)
    Business toEntity(BusinessDto dto);
}
