package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.user.*;
import org.example.banking.services.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping(ApiPaths.USERS)
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers() {
        var users = userService.getAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserRequest request) {
        var user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserStatus(@PathVariable UUID id, @Valid @RequestBody UserUpdateStatusRequest request) {
        userService.updateStatus(id, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserRole(@PathVariable UUID id, @Valid @RequestBody UserUpdateRoleRequest request) {
        userService.updateRole(id, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateDetails(id, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
