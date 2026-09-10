package org.clinica.booking.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.appointment.dto.AppointmentResponse;
import org.clinica.booking.appointment.dto.QueueSlot;
import org.clinica.booking.appointment.entity.Appointment;
import org.clinica.booking.appointment.entity.AppointmentStatus;
import org.clinica.booking.appointment.entity.RecurrenceGroup;
import org.clinica.booking.appointment.exception.AppointmentNotFoundException;
import org.clinica.booking.appointment.exception.InvalidStatusTransitionException;
import org.clinica.booking.appointment.mapper.AppointmentMapper;
import org.clinica.booking.appointment.repository.AppointmentRepository;
import org.clinica.booking.event.AppointmentCancelledEvent;
import org.clinica.booking.event.AppointmentConfirmedEvent;
import org.clinica.booking.event.AppointmentRejectedEvent;
import org.clinica.booking.event.VoucherRequestedEvent;
import org.clinica.booking.schedule.service.AvailabilityWindow;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentQueueServiceImpl implements AppointmentQueueService {

    private static final String ANOTHER_CONFIRMED_REASON = "Another request for this slot was confirmed";
    private static final String SLOT_TAKEN_REASON = "The slot was taken before this group was confirmed";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AvailabilityWindow window;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<QueueSlot> listQueue(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? window.today() : from;
        LocalDate end = to == null ? window.end() : to;

        Map<SlotKey, List<Appointment>> bySlot = appointmentRepository
                .findWithPatientByStatusInAndDateBetween(AppointmentStatus.PENDING, start, end).stream()
                .collect(Collectors.groupingBy(
                        appointment -> new SlotKey(appointment.getDate(), appointment.getStartTime()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return bySlot.entrySet().stream()
                .map(entry -> new QueueSlot(entry.getKey().date(), entry.getKey().startTime(),
                        entry.getValue().stream().map(appointmentMapper::toQueueEntry).toList()))
                .toList();
    }

    @Override
    public void requestVoucher(Long appointmentId) {
        Appointment appointment = requireStatus(appointmentId, Set.of(AppointmentStatus.SOLICITADO));
        appointment.setStatus(AppointmentStatus.PENDIENTE_COMPROBANTE);
        eventPublisher.publishEvent(new VoucherRequestedEvent(
                appointment.getId(), appointment.getPatient().getId(),
                appointment.getDate(), appointment.getStartTime()));
        log.info("Voucher requested for appointment {}", appointmentId);
    }

    @Override
    public void reject(Long appointmentId, String reason) {
        Appointment appointment = requireStatus(appointmentId, AppointmentStatus.PENDING);
        rejectAppointment(appointment, reason);
        log.info("Appointment {} rejected by the psychologist", appointmentId);
    }

    @Override
    public List<AppointmentResponse> confirm(Long appointmentId) {
        Appointment appointment = requireStatus(appointmentId, Set.of(AppointmentStatus.PENDIENTE_APROBACION));

        List<Appointment> confirmed = new ArrayList<>();
        for (Appointment occurrence : occurrencesToConfirm(appointment)) {
            // Read before write: the slot may have been taken since the group was requested
            if (slotAlreadyTaken(occurrence)) {
                rejectAppointment(occurrence, SLOT_TAKEN_REASON);
                continue;
            }
            occurrence.setStatus(AppointmentStatus.CONFIRMADO);
            eventPublisher.publishEvent(new AppointmentConfirmedEvent(
                    occurrence.getId(), occurrence.getPatient().getId(),
                    occurrence.getDate(), occurrence.getStartTime()));
            rejectRestOfQueue(occurrence);
            confirmed.add(occurrence);
        }
        log.info("Confirmed {} occurrence(s) from appointment {}", confirmed.size(), appointmentId);
        return confirmed.stream().map(appointmentMapper::toResponse).toList();
    }

    @Override
    public void cancel(Long appointmentId, String reason) {
        Appointment appointment = requireStatus(appointmentId, AppointmentStatus.LIVE);
        appointment.setStatus(AppointmentStatus.CANCELADO);
        eventPublisher.publishEvent(new AppointmentCancelledEvent(
                appointment.getId(), appointment.getPatient().getId(),
                appointment.getDate(), appointment.getStartTime(), reason));
        log.info("Appointment {} cancelled by the psychologist", appointmentId);
    }

    private List<Appointment> occurrencesToConfirm(Appointment appointment) {
        RecurrenceGroup group = appointment.getRecurrenceGroup();
        if (group == null) {
            return List.of(appointment);
        }
        return appointmentRepository.findByRecurrenceGroupIdAndStatusIn(
                group.getId(), Set.of(AppointmentStatus.PENDIENTE_APROBACION));
    }

    private boolean slotAlreadyTaken(Appointment appointment) {
        return appointmentRepository.findByDateAndStartTimeAndStatusIn(
                        appointment.getDate(), appointment.getStartTime(), Set.of(AppointmentStatus.CONFIRMADO))
                .stream()
                .anyMatch(other -> !other.getId().equals(appointment.getId()));
    }

    private void rejectRestOfQueue(Appointment confirmed) {
        appointmentRepository.findByDateAndStartTimeAndStatusIn(
                        confirmed.getDate(), confirmed.getStartTime(), AppointmentStatus.PENDING).stream()
                .filter(other -> !other.getId().equals(confirmed.getId()))
                .forEach(other -> rejectAppointment(other, ANOTHER_CONFIRMED_REASON));
    }

    private void rejectAppointment(Appointment appointment, String reason) {
        appointment.setStatus(AppointmentStatus.RECHAZADO);
        eventPublisher.publishEvent(new AppointmentRejectedEvent(
                appointment.getId(), appointment.getPatient().getId(),
                appointment.getDate(), appointment.getStartTime(), reason));
    }

    private Appointment requireStatus(Long appointmentId, Set<AppointmentStatus> expected) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        if (!expected.contains(appointment.getStatus())) {
            throw new InvalidStatusTransitionException(appointmentId, appointment.getStatus(), expected);
        }
        return appointment;
    }

    private record SlotKey(LocalDate date, LocalTime startTime) {
    }
}
