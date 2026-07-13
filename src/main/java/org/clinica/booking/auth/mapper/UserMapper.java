package org.clinica.booking.auth.mapper;

import org.clinica.booking.auth.dto.RegisterRequest;
import org.clinica.booking.auth.dto.UserResponse;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String passwordHash) {
        return User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .phone(request.phone())
                .role(Role.PATIENT)
                .build();
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole()
        );
    }
}