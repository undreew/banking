package org.example.banking.mappers;

import org.example.banking.dtos.user.UserResponse;
import org.example.banking.entities.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse respond(User user);
}
