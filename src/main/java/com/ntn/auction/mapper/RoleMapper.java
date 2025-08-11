package com.ntn.auction.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ntn.auction.dto.request.RoleRequest;
import com.ntn.auction.dto.response.RoleResponse;
import com.ntn.auction.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
