package org.clinica.booking.schedule.exception;

public class HolidayBlockNotDeletableException extends RuntimeException {

    public HolidayBlockNotDeletableException(Long id) {
        super("Holiday blocks cannot be deleted: " + id);
    }
}
