package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.llc.request.MemberMemberDto;
import com.trivine.llc.api.entity.Member;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", imports = LocalDateTime.class)
public interface MemberMapper {

    @Mappings({
            @Mapping(target = "company", ignore = true),
            @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())"),
            @Mapping(target = "isActive", constant = "true")
    })
    Member toEntity(MemberMemberDto dto);

    List<Member> toEntityList(List<MemberMemberDto> dtos);
    MemberMemberDto toDto(Member entity);
    List<MemberMemberDto> toDtoList(List<Member> entities);
}
