package org.clinica.booking.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentCancelledEvent(
        Long appointmentId,
        Long patientId,
        LocalDate date,
        LocalTime startTime,
        String reason
) {
}
