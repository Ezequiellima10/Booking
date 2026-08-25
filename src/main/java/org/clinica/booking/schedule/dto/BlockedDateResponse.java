package org.clinica.booking.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.clinica.booking.schedule.entity.BlockSource;

import java.time.LocalDate;
import java.time.LocalTime;

public record BlockedDateResponse(
        Long id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String reason,
        BlockSource source
) {
}
