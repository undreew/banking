package org.example.banking.mappers;

import org.mapstruct.Mapper;
import org.example.banking.entities.User;
import org.example.banking.dtos.user.UserResponse;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserResponse toView(User user);
}
