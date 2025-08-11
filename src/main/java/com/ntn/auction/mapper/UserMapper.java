package com.ntn.auction.mapper;

import com.ntn.auction.dto.request.UserCreationRequest;
import com.ntn.auction.dto.request.UserUpdateRequest;
import com.ntn.auction.dto.response.UserResponse;
import com.ntn.auction.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
