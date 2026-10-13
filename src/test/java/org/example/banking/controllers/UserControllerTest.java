package org.example.banking.controllers;

import org.example.banking.constants.accounts.Role;
import org.example.banking.dtos.user.UserRequest;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.dtos.user.UserUpdateRequest;
import org.example.banking.dtos.user.UserUpdateStatusRequest;
import org.example.banking.services.UserService;
import org.example.banking.support.WithMockActor;
import org.example.banking.support.WithRealSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@WithRealSecurity
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    UserService userService;

    @Test
    void getUsersWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void getUsersAsUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void getUsersAsAdminSucceeds() throws Exception {
        when(userService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void registerUserWithoutEmailIsBadRequest() throws Exception {
        var request = new UserRequest();
        request.setPassword("password1");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setRole(Role.USER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void registerUserAsAdminSucceeds() throws Exception {
        var request = new UserRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("password1");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setRole(Role.USER);

        var response = new UserResponse();
        response.setId(UUID.randomUUID());
        response.setEmail(request.getEmail());
        when(userService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));
    }

    @Test
    void updateUserStatusWithoutTokenIsUnauthorized() throws Exception {
        var request = new UserUpdateStatusRequest();
        request.setEnabled(false);
        request.setReason("Compliance hold");

        mockMvc.perform(patch("/api/v1/users/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void updateUserStatusAsUserIsForbidden() throws Exception {
        var request = new UserUpdateStatusRequest();
        request.setEnabled(false);
        request.setReason("Compliance hold");

        mockMvc.perform(patch("/api/v1/users/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void updateUserStatusAsAdminSucceeds() throws Exception {
        var request = new UserUpdateStatusRequest();
        request.setEnabled(false);
        request.setReason("Compliance hold");

        mockMvc.perform(patch("/api/v1/users/{id}/status", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateUserWithoutTokenIsUnauthorized() throws Exception {
        var request = new UserUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(put("/api/v1/users/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // Any authenticated USER (not just the owner) passes this @PreAuthorize check
    // today: PUT /users/{id} has no ownership guard (spec Section 4, backend-todo C).
    @Test
    @WithMockActor(role = "USER")
    void updateUserAsAnyAuthenticatedUserSucceeds() throws Exception {
        var request = new UserUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(put("/api/v1/users/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUserWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockActor(role = "USER")
    void deleteUserAsUserIsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockActor(role = "ADMIN")
    void deleteUserAsAdminSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
