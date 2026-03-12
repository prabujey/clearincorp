package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.SuffixMasterDTO;
import com.trivine.llc.api.entity.SuffixMaster;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SuffixMasterMapper {
    SuffixMasterMapper INSTANCE = Mappers.getMapper(SuffixMasterMapper.class);


    SuffixMasterDTO toDTO(SuffixMaster suffixMaster);

    SuffixMaster toEntity(SuffixMasterDTO suffixMasterDTO);
}
