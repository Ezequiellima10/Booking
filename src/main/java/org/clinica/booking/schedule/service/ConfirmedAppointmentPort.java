package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.ConfirmedSlot;

import java.time.LocalDate;
import java.util.List;

public interface ConfirmedAppointmentPort {

    List<ConfirmedSlot> findConfirmed(LocalDate from, LocalDate to);

    void cancelConfirmed(List<Long> appointmentIds, String reason);

    void rejectPending(LocalDate from, LocalDate to);
}
