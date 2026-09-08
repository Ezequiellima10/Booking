package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.ConfirmedSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ConfirmedAppointmentPort {

    List<ConfirmedSlot> findConfirmed(LocalDate from, LocalDate to);

    void cancelConfirmed(List<Long> appointmentIds, String reason);

    // Null times mean full day
    void rejectPending(LocalDate from, LocalDate to, LocalTime startTime, LocalTime endTime);
}
