package org.clinica.booking.patient.exception;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(Long id) {
        super("Patient not found: " + id);
    }
}