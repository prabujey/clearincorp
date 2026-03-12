package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.AssignedTaskMasterDto;
import com.trivine.llc.api.entity.AssignedTaskMaster;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.LoginUser;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssignedTaskMasterMapper {
    @Mapping(source = "createdBy.loginUserId", target = "createdById")
    AssignedTaskMasterDto toDto(AssignedTaskMaster e);
    default Page<AssignedTaskMasterDto> toDtoPage(Page<AssignedTaskMaster> page) {
        return page.map(this::toDto);
    }

    List<AssignedTaskMasterDto> toDtoList(List<AssignedTaskMaster> entities);

    // DTO -> Entity (create/put)
    @Mapping(target = "createdBy", source = "createdById", qualifiedByName = "idToLoginUser")
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    AssignedTaskMaster toEntity(AssignedTaskMasterDto dto);

    // PATCH-style update (ignore nulls)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    void updateEntityFromDto(AssignedTaskMasterDto dto, @MappingTarget AssignedTaskMaster entity);

    // Helper: Long -> LoginUser reference
    @Named("idToLoginUser")
    default LoginUser idToLoginUser(Long id) {
        if (id == null) return null;
        LoginUser u = new LoginUser();
        u.setLoginUserId(id);
        return u;
    }
}
