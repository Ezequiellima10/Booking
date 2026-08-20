package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.DayAvailability;
import org.clinica.booking.schedule.dto.DaySchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AvailabilityService {

    List<DayAvailability> getRange(LocalDate from, LocalDate to);

    DaySchedule getDay(LocalDate date);

    boolean isSlotAvailable(LocalDate date, LocalTime startTime);

    List<LocalDate> findAvailableDates(DayOfWeek dayOfWeek, LocalTime startTime);
}
