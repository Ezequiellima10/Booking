package org.clinica.booking.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentConfirmedEvent(
        Long appointmentId,
        Long patientId,
        LocalDate date,
        LocalTime startTime
) {
}
