package org.clinica.booking.appointment.exception;

public class CancellationWindowClosedException extends RuntimeException {

    public CancellationWindowClosedException(Long appointmentId) {
        super("Appointment " + appointmentId + " starts within 24 hours and can no longer be cancelled");
    }
}
