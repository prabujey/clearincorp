package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.EmailTokenDto;
import com.trivine.llc.api.entity.EmailToken;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EmailTokenMapper {
    EmailTokenMapper INSTANCE = Mappers.getMapper(EmailTokenMapper.class);

    EmailTokenDto toDto(EmailToken entity);

    EmailToken toEntity(EmailTokenDto dto);
}