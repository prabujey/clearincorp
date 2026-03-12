package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.FilingFailureDTO;
import com.trivine.llc.api.entity.FilingFailure;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface FilingFailureMapper {


    FilingFailure toEntity(FilingFailureDTO filingFailureDTO);


    FilingFailureDTO toDto(FilingFailure filingFailure);

    List<FilingFailureDTO> toDtoList(List<FilingFailure> filingFailureList);
    List<FilingFailure> toEntityList(List<FilingFailureDTO> filingFailureDTOList);
}

