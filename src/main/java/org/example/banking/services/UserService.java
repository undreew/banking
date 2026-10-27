package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.example.banking.constants.audit.AuditAction;
import org.example.banking.dtos.user.UserRequest;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.user.UserUpdateRequest;
import org.example.banking.dtos.user.UserUpdateRoleRequest;
import org.example.banking.dtos.user.UserUpdateStatusRequest;
import org.example.banking.entities.User;
import org.example.banking.exceptions.ConflictException;
import org.example.banking.exceptions.NotFoundException;
import org.example.banking.mappers.UserMapper;
import org.example.banking.repositories.RefreshTokenRepository;
import org.example.banking.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TARGET_USER = "User";

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository sessions;
    private final AuditService auditService;
    private final UserMapper userMapper;

    public UserResponse create(UserRequest request) {
        var user = User.create(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                passwordEncoder.encode(request.getPassword())
        );
        user.updateRole(request.getRole());
        userRepository.save(user);
        return userMapper.respond(user);
    }

    /** Disabling a user ends every one of their sessions immediately. */
    @Transactional
    public void updateStatus(UUID id, UserUpdateStatusRequest request, UUID actorId) {
        var user = getExistingUser(id);
        user.enableUser(request.getEnabled());

        if (!request.getEnabled()) {
            sessions.revokeAllForUser(id.toString(), Instant.now());
        }

        auditService.record(
                actorId,
                AuditAction.STATUS_CHANGED,
                TARGET_USER,
                id,
                "enabled=" + request.getEnabled() + "; " + request.getReason(),
                null
        );
    }

    /** The role is carried in the access token, so a change revokes sessions to force a new login. */
    @Transactional
    public void updateRole(UUID id, UserUpdateRoleRequest request, UUID actorId) {
        var user = getExistingUser(id);
        user.updateRole(request.getRole());
        sessions.revokeAllForUser(id.toString(), Instant.now());

        auditService.record(
                actorId,
                AuditAction.ROLE_CHANGED,
                TARGET_USER,
                id,
                "role=" + request.getRole() + "; " + request.getReason(),
                null
        );
    }

    @Transactional
    public void updateDetails(UUID id, UserUpdateRequest request) {
        var user = getExistingUser(id);
        user.updateDetails(request.getFirstName(), request.getLastName());
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::respond)
                .toList();
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        if (id.equals(actorId)) {
            throw new ConflictException("You cannot delete your own account");
        }

        getExistingUser(id);
        userRepository.deleteById(id); // refresh_tokens rows cascade on delete (V5)
    }

    @Transactional
    public UserResponse updateMyProfile(UserUpdateRequest request, UUID actorId) {
        var user = getExistingUser(actorId);
        user.updateDetails(request.getFirstName(), request.getLastName());
        return userMapper.respond(user);
    }

    private User getExistingUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }
}