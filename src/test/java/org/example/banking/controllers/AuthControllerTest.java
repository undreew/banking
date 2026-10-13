package org.example.banking.controllers;

import org.example.banking.dtos.auth.AuthenticationResponse;
import org.example.banking.dtos.auth.LoginRequest;
import org.example.banking.dtos.auth.RegisterRequest;
import org.example.banking.dtos.user.UserResponse;
import org.example.banking.services.AuthService;
import org.example.banking.support.WithRealSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithRealSecurity
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @MockitoBean
    AuthService authService;

    @Test
    void registerWithInvalidPasswordIsBadRequest() throws Exception {
        var request = new RegisterRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("allsmall1");
        request.setFirstName("Jane");
        request.setLastName("Doe");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registerSucceedsWithoutAnyToken() throws Exception {
        var request = new RegisterRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("Password1");
        request.setFirstName("Jane");
        request.setLastName("Doe");

        var response = new UserResponse();
        response.setEmail(request.getEmail());
        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void loginWithWrongCredentialsIsUnauthorized() throws Exception {
        var request = new LoginRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("wrongpass");

        when(authService.login(any())).thenThrow(new org.example.banking.exceptions.UnauthenticatedException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSucceedsAndSetsRefreshCookie() throws Exception {
        var request = new LoginRequest();
        request.setEmail("jane.doe@example.com");
        request.setPassword("correctpass");

        var user = new UserResponse();
        user.setEmail(request.getEmail());
        var response = new AuthenticationResponse("access-token", "refresh-token",
                Duration.ofMinutes(15), Duration.ofDays(7), user);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCookieIsUnauthorized() throws Exception {
        // Documents a known gotcha (spec Section 7): the design intends a missing
        // cookie to mean "already logged out", but AuthController.logout() calls
        // extractCookie() unconditionally, which itself always throws when the cookie
        // is missing rather than returning null — so the 401 below is real, current
        // behavior, not the designed one. This test exists to catch an accidental
        // behavior change, not to assert this is correct.
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized());
    }
}
