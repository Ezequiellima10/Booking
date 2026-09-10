package org.clinica.booking.appointment.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class SlotNotAvailableException extends RuntimeException {

    public SlotNotAvailableException(LocalDate date, LocalTime startTime) {
        super("Slot not available: " + date + " " + startTime);
    }
}
