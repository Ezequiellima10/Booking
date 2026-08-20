package org.clinica.booking.schedule.dto;

import java.time.LocalDate;

public record DayAvailability(LocalDate date, int freeSlots) {
}
