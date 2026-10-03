package org.example.banking.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.example.banking.entities.User;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.auth.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public abstract class AuthMapper {
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(target = "password", expression = "java(passwordEncoder.encode(request.getPassword()))")
    public abstract User create(RegisterRequest request);

    public abstract UserResponse toView(User user);
}
