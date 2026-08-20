package org.clinica.booking.schedule.dto;

import java.time.LocalTime;

public record TimeSlot(LocalTime startTime, LocalTime endTime, boolean available) {
}
