package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.UserCompanyDto;
import com.trivine.llc.api.entity.UserCompany;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserCompanyMapper {

    UserCompany toEntity(UserCompanyDto userCompanyDto);

    UserCompanyDto toDto(UserCompany userCompany);

    List<UserCompanyDto> toDtoList(List<UserCompany> userCompanyList);

    List<UserCompany> toEntityList(List<UserCompanyDto> userCompanyDtoList);
}
