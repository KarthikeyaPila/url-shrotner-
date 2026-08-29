package com.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository repository;

    private PasswordEncoder passwordEncoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        service = new AuthService(repository, passwordEncoder);
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(repository.existsByEmail("person@example.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.register(new RegisterRequest(
                " Person@Example.com ", "correct horse battery staple", " Person "
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("person@example.com");
        assertThat(saved.getDisplayName()).isEqualTo("Person");
        assertThat(saved.getPassword()).isNotEqualTo("correct horse battery staple");
        assertThat(passwordEncoder.matches("correct horse battery staple", saved.getPassword())).isTrue();
        assertThat(response.email()).isEqualTo("person@example.com");
        assertThat(response.displayName()).isEqualTo("Person");
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(repository.existsByEmail("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "person@example.com", "correct horse battery staple", "Person"
        )))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("An account with that email already exists");
    }
}
