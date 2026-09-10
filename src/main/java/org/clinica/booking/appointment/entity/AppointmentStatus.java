package org.clinica.booking.appointment.entity;

import java.util.Set;

public enum AppointmentStatus {
    SOLICITADO,
    PENDIENTE_COMPROBANTE,
    PENDIENTE_APROBACION,
    CONFIRMADO,
    RECHAZADO,
    CANCELADO,
    FINALIZADO;

    // Live but not occupying the slot yet
    public static final Set<AppointmentStatus> PENDING =
            Set.of(SOLICITADO, PENDIENTE_COMPROBANTE, PENDIENTE_APROBACION);

    // Mirrors uq_appointments_patient_live_day
    public static final Set<AppointmentStatus> LIVE =
            Set.of(SOLICITADO, PENDIENTE_COMPROBANTE, PENDIENTE_APROBACION, CONFIRMADO);

    public boolean isLive() {
        return LIVE.contains(this);
    }
}
