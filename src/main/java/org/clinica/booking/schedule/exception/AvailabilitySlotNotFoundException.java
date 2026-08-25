package org.clinica.booking.schedule.exception;

public class AvailabilitySlotNotFoundException extends RuntimeException {

    public AvailabilitySlotNotFoundException(Long id) {
        super("Availability slot not found: " + id);
    }
}
