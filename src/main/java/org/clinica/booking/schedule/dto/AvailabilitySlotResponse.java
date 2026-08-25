package org.clinica.booking.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.clinica.booking.schedule.entity.SlotType;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilitySlotResponse(
        Long id,
        SlotType type,
        DayOfWeek dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        String reason
) {
}
