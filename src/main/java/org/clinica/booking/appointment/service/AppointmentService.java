package org.clinica.booking.appointment.service;

import org.clinica.booking.appointment.dto.AppointmentRequest;
import org.clinica.booking.appointment.dto.AppointmentResponse;
import org.clinica.booking.appointment.dto.RecurrencePreview;
import org.clinica.booking.appointment.dto.RecurrenceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse request(Long patientId, AppointmentRequest request);

    RecurrencePreview previewRecurrence(Long patientId, RecurrenceRequest request);

    List<AppointmentResponse> requestRecurrence(Long patientId, RecurrenceRequest request);

    Page<AppointmentResponse> listOwn(Long patientId, Pageable pageable);

    void cancel(Long patientId, Long appointmentId);
}
