package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.llc.request.ManagerMemberDto;

import com.trivine.llc.api.entity.ManagerMember;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", imports = LocalDateTime.class)
public interface ManagerMemberMapper {

    @Mappings({
            @Mapping(target = "company", ignore = true),
            @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())"),
            @Mapping(target = "isActive", constant = "true")
    })
    ManagerMember toEntity(ManagerMemberDto dto);

    List<ManagerMember> toEntityList(List<ManagerMemberDto> dtos);
    ManagerMemberDto toDto(ManagerMember entity);
    List<ManagerMemberDto> toDtoList(List<ManagerMember> entities);

}
