package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.EinDetailsDto;
import com.trivine.llc.api.entity.Company;
import com.trivine.llc.api.entity.EinDetails;
import com.trivine.llc.api.entity.ReasonForApplying;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface EinDetailsMapper {

    // Entity -> DTO
    @Mapping(source = "company.companyId",            target = "companyId")
    @Mapping(source = "reasonForApplying.id",         target = "reasonForApplyingId")
    EinDetailsDto toDto(EinDetails entity);

    // DTO -> Entity
    @Mapping(source = "companyId",            target = "company", qualifiedByName = "idToCompany")
    @Mapping(source = "reasonForApplyingId",  target = "reasonForApplying", qualifiedByName = "idToReason")
    EinDetails toEntity(EinDetailsDto dto);

    // PATCH update (ignore nulls)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "companyId",            target = "company", qualifiedByName = "idToCompany")
    @Mapping(source = "reasonForApplyingId",  target = "reasonForApplying", qualifiedByName = "idToReason")
    void updateEntityFromDto(EinDetailsDto dto, @MappingTarget EinDetails entity);

    @Named("idToCompany")
    default Company idToCompany(Long id) {
        if (id == null) return null;
        Company c = new Company();
        c.setCompanyId(id);
        return c;
    }

    @Named("idToReason")
    default ReasonForApplying idToReason(Long id) {
        if (id == null) return null;
        ReasonForApplying r = new ReasonForApplying();
        r.setId(id);
        return r;
    }
}
