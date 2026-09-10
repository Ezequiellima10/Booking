package org.clinica.booking.appointment.exception;

import org.clinica.booking.appointment.entity.AppointmentStatus;

import java.util.Collection;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Long appointmentId, AppointmentStatus current,
                                            Collection<AppointmentStatus> expected) {
        super("Appointment " + appointmentId + " is " + current + ", expected one of " + expected);
    }
}
