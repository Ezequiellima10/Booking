package org.clinica.booking.patient.dto;

import java.time.OffsetDateTime;

public record PatientResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        OffsetDateTime createdAt
) {
}