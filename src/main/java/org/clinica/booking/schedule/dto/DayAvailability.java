package org.clinica.booking.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record DayAvailability(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        int freeSlots
) {
}
