package org.example.banking.mappers;

import org.example.banking.dtos.user.UserUpdateRequest;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.user.UserRequest;
import org.example.banking.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User create(UserRequest request);
    UserResponse respond(User user);
    void update(UserUpdateRequest request, @MappingTarget User user);
}
