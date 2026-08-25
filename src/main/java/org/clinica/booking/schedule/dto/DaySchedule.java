package org.clinica.booking.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record DaySchedule(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        List<TimeSlot> slots
) {
}
