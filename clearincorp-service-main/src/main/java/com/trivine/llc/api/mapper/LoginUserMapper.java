package com.trivine.llc.api.mapper;


import com.trivine.llc.api.entity.LoginUser;
import com.trivine.llc.api.dto.LoginUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses           = { RoleMapper.class, UserCompanyMapper.class },
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LoginUserMapper {
    LoginUserDto toDto(LoginUser entity);
    LoginUser   toEntity(LoginUserDto dto);


    List<LoginUserDto> toDtoList(List<LoginUser> LoginUserList);
    List<LoginUser> toEntityList(List<LoginUserDto> LoginUserDtoList);

}

