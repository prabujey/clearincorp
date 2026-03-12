package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.FilingFailureCategoryDTO;
import com.trivine.llc.api.entity.FilingFailureCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FilingFailureCategoryMapper {


    // Method to map entity to DTO
    FilingFailureCategoryDTO toDTO(FilingFailureCategory entity);

    // Method to map DTO to entity
    FilingFailureCategory toEntity(FilingFailureCategoryDTO dto);
}

