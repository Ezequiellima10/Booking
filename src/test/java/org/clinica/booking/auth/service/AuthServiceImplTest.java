package org.clinica.booking.auth.service;

import org.clinica.booking.auth.dto.AuthResponse;
import org.clinica.booking.auth.dto.LoginRequest;
import org.clinica.booking.auth.dto.RegisterRequest;
import org.clinica.booking.auth.dto.UserResponse;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.exception.EmailAlreadyRegisteredException;
import org.clinica.booking.auth.exception.InvalidCredentialsException;
import org.clinica.booking.auth.mapper.UserMapper;
import org.clinica.booking.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("patient@example.com")
                .passwordHash("hashed")
                .firstName("Ana")
                .lastName("Gomez")
                .role(Role.PATIENT)
                .build();
    }

    @Test
    void register_savesUserAndReturnsTokenAndResponse() {
        RegisterRequest request = new RegisterRequest("Patient@Example.com ", "password123", "Ana", "Gomez", null);
        User user = buildUser();
        UserResponse userResponse = new UserResponse(1L, user.getEmail(), "Ana", "Gomez", null, Role.PATIENT);

        when(userRepository.existsByEmail("patient@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userMapper.toEntity(request, "hashed")).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isEqualTo(userResponse);
        verify(userRepository).save(user);
    }

    @Test
    void register_emailAlreadyRegistered_throwsAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest("patient@example.com", "password123", "Ana", "Gomez", null);
        when(userRepository.existsByEmail("patient@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsTokenAndResponse() {
        LoginRequest request = new LoginRequest("Patient@Example.com", "password123");
        User user = buildUser();
        UserResponse userResponse = new UserResponse(1L, user.getEmail(), "Ana", "Gomez", null, Role.PATIENT);

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user()).isEqualTo(userResponse);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        LoginRequest request = new LoginRequest("patient@example.com", "wrong-password");
        User user = buildUser();

        when(userRepository.findByEmail("patient@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void getCurrentUser_delegatesToMapper() {
        User user = buildUser();
        UserResponse userResponse = new UserResponse(1L, user.getEmail(), "Ana", "Gomez", null, Role.PATIENT);
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        UserResponse result = authService.getCurrentUser(user);

        assertThat(result).isEqualTo(userResponse);
    }
}