package org.clinica.booking.appointment.exception;

import java.time.LocalDate;

public class DailyLimitReachedException extends RuntimeException {

    public DailyLimitReachedException(LocalDate date) {
        super("The patient already has a live appointment on " + date);
    }
}
