package com.trivine.llc.api.mapper;


import com.trivine.llc.api.entity.CompanyFiling;
import com.trivine.llc.api.dto.CompanyFilingDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CompanyFilingMapper {
    CompanyFilingDto toDto(CompanyFiling entity);
    CompanyFiling toEntity(CompanyFilingDto dto);
}

