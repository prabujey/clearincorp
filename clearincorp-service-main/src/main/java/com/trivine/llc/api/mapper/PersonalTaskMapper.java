package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.PersonalTaskCreateDto;
import com.trivine.llc.api.dto.PersonalTaskResponseDto;
import com.trivine.llc.api.dto.PersonalTaskUpdateDto;
import com.trivine.llc.api.entity.PersonalTask;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonalTaskMapper {

    PersonalTask toEntity(PersonalTaskCreateDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(PersonalTaskUpdateDto dto, @MappingTarget PersonalTask entity);

    PersonalTaskResponseDto toResponseDto(PersonalTask entity);
}

