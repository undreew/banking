package org.example.banking.services;

import lombok.AllArgsConstructor;
import org.example.banking.dtos.user.*;
import org.example.banking.entities.User;
import org.example.banking.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.example.banking.mappers.UserMapper;
import org.example.banking.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse create(UserRequest request) {
        var user = User.create(request.getEmail(), request.getFirstName(), request.getLastName(), passwordEncoder.encode(request.getPassword()));
        user.updateRole(request.getRole());
        userRepository.save(user);
        return userMapper.respond(user);
    }

    public void updateStatus(UUID id, UserUpdateStatusRequest request) {
        var user = getExistingUser(id);
        user.enableUser(request.getEnabled());
        userRepository.save(user);
    }

    public void updateRole(UUID id, UserUpdateRoleRequest request) {
        var user = getExistingUser(id);
        user.updateRole(request.getRole());
        userRepository.save(user);
    }

    public void updateDetails(UUID id, UserUpdateRequest request) {
        var user = getExistingUser(id);
        user.updateDetails(request.getFirstName(), request.getLastName());
        userRepository.save(user);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(userMapper::respond).collect(Collectors.toList());
    }

    public void delete(UUID id) {
        getExistingUser(id);
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponse updateMyProfile(UserUpdateRequest request, UUID actorId) {
        var user = getExistingUser(actorId);
        user.updateDetails(request.getFirstName(), request.getLastName());
        userRepository.save(user);
        return userMapper.respond(user);
    }

    private User getExistingUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("Account", id));
    }
}
