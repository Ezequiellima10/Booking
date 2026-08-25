package org.clinica.booking.schedule.exception;

public class BlockedDateNotFoundException extends RuntimeException {

    public BlockedDateNotFoundException(Long id) {
        super("Blocked date not found: " + id);
    }
}
