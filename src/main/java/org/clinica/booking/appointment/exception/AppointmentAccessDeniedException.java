package org.clinica.booking.appointment.exception;

public class AppointmentAccessDeniedException extends RuntimeException {

    public AppointmentAccessDeniedException(Long appointmentId) {
        super("Appointment " + appointmentId + " does not belong to the patient");
    }
}
