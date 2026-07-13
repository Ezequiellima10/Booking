package org.clinica.booking.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.auth.dto.AuthResponse;
import org.clinica.booking.auth.dto.LoginRequest;
import org.clinica.booking.auth.dto.RegisterRequest;
import org.clinica.booking.auth.dto.UserResponse;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.exception.EmailAlreadyRegisteredException;
import org.clinica.booking.auth.exception.InvalidCredentialsException;
import org.clinica.booking.auth.mapper.UserMapper;
import org.clinica.booking.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        User user = userMapper.toEntity(request, passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        log.info("User registered: id={}, role={}", user.getId(), user.getRole());

        return new AuthResponse(jwtService.generateToken(user), userMapper.toResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.debug("Failed login attempt for user id={}", user.getId());
            throw new InvalidCredentialsException();
        }

        log.info("User logged in: id={}, role={}", user.getId(), user.getRole());
        return new AuthResponse(jwtService.generateToken(user), userMapper.toResponse(user));
    }

    @Override
    public UserResponse getCurrentUser(User user) {
        return userMapper.toResponse(user);
    }
}
