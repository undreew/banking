package org.example.banking.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.example.banking.repositories.AuthRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final AuthService authService;

    public void register() {
    }

    public void login() {
    }

    public void refreshToken() {
    }

    public void logout() {
    }
}
