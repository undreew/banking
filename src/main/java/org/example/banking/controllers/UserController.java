package org.example.banking.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.banking.constants.ApiPaths;
import org.example.banking.dtos.auth.AuthenticatedActor;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.user.UserUpdateRequest;
import org.example.banking.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-service profile operations. Administration of other users lives in {@link AdminUserController}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.USERS)
public class UserController {

    private final UserService userService;

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyProfile(    
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedActor actor
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(request, actor.id()));
    }
}