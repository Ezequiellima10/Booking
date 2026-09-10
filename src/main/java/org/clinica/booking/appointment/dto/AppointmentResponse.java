package org.clinica.booking.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.clinica.booking.appointment.entity.AppointmentStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentResponse(
        Long id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        AppointmentStatus status,
        Long recurrenceGroupId
) {
}
