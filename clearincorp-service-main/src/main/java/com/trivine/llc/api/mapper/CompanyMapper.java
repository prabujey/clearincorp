package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.CompanyDto;
import com.trivine.llc.api.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompanyMapper {
    Company toEntity(CompanyDto companyDto);
    CompanyDto toDto(Company company);
    List<CompanyDto> toDtoList(List<Company> companyList);
    List<Company> toEntityList(List<CompanyDto> companyDtoList);
}
