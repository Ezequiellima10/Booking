package org.clinica.booking.appointment.exception;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class NoAvailableOccurrencesException extends RuntimeException {

    public NoAvailableOccurrencesException(DayOfWeek dayOfWeek, LocalTime startTime) {
        super("No available occurrences for " + dayOfWeek + " at " + startTime);
    }
}
