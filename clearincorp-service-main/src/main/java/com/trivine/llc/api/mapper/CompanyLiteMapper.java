package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.CompanyLiteDto;
import com.trivine.llc.api.dto.LoginUserLiteDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.LoginUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompanyLiteMapper {

    CompanyLiteDto toDto(Company company);

    // Explicit sub-mapping for clarity (optional; MapStruct can infer if names match)
    @Mapping(source = "loginUserId",  target = "loginUserId")
    @Mapping(source = "firstName",   target = "firstName")
    @Mapping(source = "lastName",    target = "lastName")
    @Mapping(source = "email",        target = "email")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    LoginUserLiteDto toDto(LoginUser loginUser);
}