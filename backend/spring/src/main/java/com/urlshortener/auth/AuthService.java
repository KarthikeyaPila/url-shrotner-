package com.urlshortener.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        return UserResponse.from(repository.save(user));
    }
}
