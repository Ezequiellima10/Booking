package org.clinica.booking.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RecurrencePreview(
        DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd") List<LocalDate> availableDates,
        @JsonFormat(pattern = "yyyy-MM-dd") List<LocalDate> unavailableDates
) {
}
