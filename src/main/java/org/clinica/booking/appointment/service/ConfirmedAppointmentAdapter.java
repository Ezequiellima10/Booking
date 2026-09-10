package org.clinica.booking.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.appointment.entity.Appointment;
import org.clinica.booking.appointment.entity.AppointmentStatus;
import org.clinica.booking.appointment.mapper.AppointmentMapper;
import org.clinica.booking.appointment.repository.AppointmentRepository;
import org.clinica.booking.event.AppointmentCancelledEvent;
import org.clinica.booking.event.AppointmentRejectedEvent;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.service.ConfirmedAppointmentPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ConfirmedAppointmentAdapter implements ConfirmedAppointmentPort {

    private static final String BLOCKED_DATE_REASON = "The psychologist blocked this date";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ConfirmedSlot> findConfirmed(LocalDate from, LocalDate to) {
        return appointmentRepository
                .findWithPatientByStatusAndDateBetween(AppointmentStatus.CONFIRMADO, from, to).stream()
                .map(this::toConfirmedSlot)
                .toList();
    }

    @Override
    public void cancelConfirmed(List<Long> appointmentIds, String reason) {
        List<Appointment> affected = appointmentRepository.findAllById(appointmentIds).stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMADO)
                .toList();

        affected.forEach(appointment -> {
            appointment.setStatus(AppointmentStatus.CANCELADO);
            eventPublisher.publishEvent(new AppointmentCancelledEvent(
                    appointment.getId(), appointment.getPatient().getId(),
                    appointment.getDate(), appointment.getStartTime(), reason));
        });
        log.info("Cancelled {} confirmed appointments to make room for a block", affected.size());
    }

    @Override
    public void rejectPending(LocalDate from, LocalDate to, LocalTime startTime, LocalTime endTime) {
        List<Appointment> affected = appointmentRepository
                .findWithPatientByStatusInAndDateBetween(AppointmentStatus.PENDING, from, to).stream()
                .filter(appointment -> overlaps(appointment, startTime, endTime))
                .toList();

        affected.forEach(appointment -> {
            appointment.setStatus(AppointmentStatus.RECHAZADO);
            eventPublisher.publishEvent(new AppointmentRejectedEvent(
                    appointment.getId(), appointment.getPatient().getId(),
                    appointment.getDate(), appointment.getStartTime(), BLOCKED_DATE_REASON));
        });
        log.info("Rejected {} pending requests covered by a block between {} and {}", affected.size(), from, to);
    }

    private ConfirmedSlot toConfirmedSlot(Appointment appointment) {
        return new ConfirmedSlot(
                appointment.getId(),
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointmentMapper.fullName(appointment.getPatient())
        );
    }

    // Null times mean full day
    private boolean overlaps(Appointment appointment, LocalTime startTime, LocalTime endTime) {
        return startTime == null
                || (appointment.getStartTime().isBefore(endTime) && appointment.getEndTime().isAfter(startTime));
    }
}
