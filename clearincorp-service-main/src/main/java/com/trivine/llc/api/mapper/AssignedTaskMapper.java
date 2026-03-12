package com.trivine.llc.api.mapper;

import com.trivine.llc.api.dto.AssignedTaskDto;
import com.trivine.llc.api.entity.AssignedTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssignedTaskMapper {
    @Mapping(source = "master.id", target = "masterId")
    @Mapping(source = "assignee.loginUserId", target = "assigneeId")
    AssignedTaskDto toDto(AssignedTask e);
}
