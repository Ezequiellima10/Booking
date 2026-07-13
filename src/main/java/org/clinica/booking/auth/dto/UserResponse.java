package org.clinica.booking.auth.dto;

import org.clinica.booking.auth.entity.Role;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role
) {
}