package org.clinica.booking.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.clinica.booking.schedule.entity.SlotType;
import org.clinica.booking.schedule.validation.ValidTimeRange;

import java.time.DayOfWeek;
import java.time.LocalTime;

@ValidTimeRange
public record AvailabilitySlotRequest(
        @NotNull
        SlotType type,

        @NotNull
        DayOfWeek dayOfWeek,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @Size(max = 255)
        String reason
) {
}
