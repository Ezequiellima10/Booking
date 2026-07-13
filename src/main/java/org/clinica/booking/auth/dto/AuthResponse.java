package org.clinica.booking.auth.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}