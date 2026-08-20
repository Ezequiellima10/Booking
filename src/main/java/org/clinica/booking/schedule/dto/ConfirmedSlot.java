package org.clinica.booking.schedule.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmedSlot(
        Long appointmentId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String patientFullName
) {
}
