package org.clinica.booking.auth.service;

import org.clinica.booking.auth.dto.AuthResponse;
import org.clinica.booking.auth.dto.LoginRequest;
import org.clinica.booking.auth.dto.RegisterRequest;
import org.clinica.booking.auth.dto.UserResponse;
import org.clinica.booking.auth.entity.User;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(User user);
}
