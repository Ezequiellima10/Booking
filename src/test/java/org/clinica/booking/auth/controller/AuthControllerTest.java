package org.clinica.booking.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clinica.booking.auth.dto.AuthResponse;
import org.clinica.booking.auth.dto.LoginRequest;
import org.clinica.booking.auth.dto.RegisterRequest;
import org.clinica.booking.auth.dto.UserResponse;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.exception.EmailAlreadyRegisteredException;
import org.clinica.booking.auth.exception.InvalidCredentialsException;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.auth.service.AuthService;
import org.clinica.booking.auth.service.JwtService;
import org.clinica.booking.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;


    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

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
    void register_validRequest_returns201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest("patient@example.com", "password123", "Ana", "Gomez", null);
        UserResponse userResponse = new UserResponse(1L, "patient@example.com", "Ana", "Gomez", null, Role.PATIENT);
        when(authService.register(any())).thenReturn(new AuthResponse("jwt-token", userResponse));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("patient@example.com"));
    }

    @Test
    void register_blankFields_returns400WithFieldErrors() throws Exception {
        RegisterRequest request = new RegisterRequest("", "short", "", "Gomez", null);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest("patient@example.com", "password123", "Ana", "Gomez", null);
        when(authService.register(any())).thenThrow(new EmailAlreadyRegisteredException("patient@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered: patient@example.com"));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("patient@example.com", "password123");
        UserResponse userResponse = new UserResponse(1L, "patient@example.com", "Ana", "Gomez", null, Role.PATIENT);
        when(authService.login(any())).thenReturn(new AuthResponse("jwt-token", userResponse));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("patient@example.com", "wrong-password");
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void me_authenticatedUser_returnsCurrentUser() throws Exception {
        User user = buildUser();
        UserResponse userResponse = new UserResponse(1L, "patient@example.com", "Ana", "Gomez", null, Role.PATIENT);
        when(authService.getCurrentUser(user)).thenReturn(userResponse);

        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

        mockMvc.perform(get("/api/auth/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"));
    }

    @Test
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}