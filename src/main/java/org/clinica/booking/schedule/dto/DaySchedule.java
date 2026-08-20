package org.clinica.booking.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record DaySchedule(LocalDate date, List<TimeSlot> slots) {
}
