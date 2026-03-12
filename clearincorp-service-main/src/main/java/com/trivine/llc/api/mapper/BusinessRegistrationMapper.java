package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.BusinessDto;
import com.trivine.llc.api.dto.BusinessOwnerDto;
import com.trivine.llc.api.dto.response.BusinessRegistrationDto;
import com.trivine.llc.api.entity.Business;
import com.trivine.llc.api.entity.BusinessOwner;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BusinessRegistrationMapper {

    BusinessOwnerDto toOwnerDto(BusinessOwner entity);
    BusinessOwner toOwnerEntity(BusinessOwnerDto dto);

    @Mapping(target = "ownerId", source = "owner.ownerId")
    @Mapping(target = "serviceId", source = "service.serviceId")
    @Mapping(target = "serviceName", source = "service.serviceName")
    BusinessDto toBusinessDto(Business entity);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "service", ignore = true)
    Business toBusinessEntity(BusinessDto dto);

    @AfterMapping
    default void fillRegistrationDto(@MappingTarget BusinessRegistrationDto target, Business source) {
    }

    default BusinessRegistrationDto toRegistrationDto(Business entity) {
        if (entity == null) return null;
        BusinessOwnerDto ownerDto = (entity.getOwner() != null) ? toOwnerDto(entity.getOwner()) : null;
        BusinessDto businessDto = toBusinessDto(entity);
        return BusinessRegistrationDto.builder()
                .owner(ownerDto)
                .business(businessDto)
                .build();
    }
}
