package org.clinica.booking.appointment.service;

import org.clinica.booking.appointment.dto.AppointmentResponse;
import org.clinica.booking.appointment.dto.QueueSlot;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentQueueService {

    List<QueueSlot> listQueue(LocalDate from, LocalDate to);

    void requestVoucher(Long appointmentId);

    void reject(Long appointmentId, String reason);

    List<AppointmentResponse> confirm(Long appointmentId);

    void cancel(Long appointmentId, String reason);
}
