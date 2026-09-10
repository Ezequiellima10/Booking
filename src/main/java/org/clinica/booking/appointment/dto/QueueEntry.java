package org.clinica.booking.appointment.dto;

import org.clinica.booking.appointment.entity.AppointmentStatus;

import java.time.OffsetDateTime;

public record QueueEntry(
        Long appointmentId,
        String patientFullName,
        AppointmentStatus status,
        OffsetDateTime requestedAt
) {
}
