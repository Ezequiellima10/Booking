package org.clinica.booking.schedule.dto;

import java.time.LocalDate;
import java.util.List;

public record HolidayConflict(LocalDate date, String holidayName, List<ConfirmedSlot> appointments) {
}
