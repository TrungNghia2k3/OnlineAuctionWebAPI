package com.ntn.auction.mapper;

import org.mapstruct.Mapper;

import com.ntn.auction.dto.request.PermissionRequest;
import com.ntn.auction.dto.response.PermissionResponse;
import com.ntn.auction.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
