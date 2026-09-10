package org.clinica.booking.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RecurrenceRequest(
        @NotNull
        DayOfWeek dayOfWeek,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime
) {
}
