package org.clinica.booking.appointment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.appointment.dto.AppointmentRequest;
import org.clinica.booking.appointment.dto.AppointmentResponse;
import org.clinica.booking.appointment.dto.RecurrencePreview;
import org.clinica.booking.appointment.dto.RecurrenceRequest;
import org.clinica.booking.appointment.entity.Appointment;
import org.clinica.booking.appointment.entity.AppointmentStatus;
import org.clinica.booking.appointment.entity.RecurrenceGroup;
import org.clinica.booking.appointment.exception.AppointmentAccessDeniedException;
import org.clinica.booking.appointment.exception.AppointmentNotFoundException;
import org.clinica.booking.appointment.exception.CancellationWindowClosedException;
import org.clinica.booking.appointment.exception.DailyLimitReachedException;
import org.clinica.booking.appointment.exception.InvalidStatusTransitionException;
import org.clinica.booking.appointment.exception.NoAvailableOccurrencesException;
import org.clinica.booking.appointment.exception.SlotNotAvailableException;
import org.clinica.booking.appointment.mapper.AppointmentMapper;
import org.clinica.booking.appointment.repository.AppointmentRepository;
import org.clinica.booking.appointment.repository.RecurrenceGroupRepository;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.event.AppointmentRequestedEvent;
import org.clinica.booking.schedule.dto.TimeSlot;
import org.clinica.booking.schedule.service.AvailabilityService;
import org.clinica.booking.schedule.service.AvailabilityWindow;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private static final Duration CANCELLATION_THRESHOLD = Duration.ofHours(24);

    private final AppointmentRepository appointmentRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final AvailabilityWindow window;
    private final AppointmentMapper appointmentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public AppointmentResponse request(Long patientId, AppointmentRequest request) {
        TimeSlot slot = freeSlot(request.date(), request.startTime());
        rejectIfDayTaken(patientId, request.date());

        Appointment saved = appointmentRepository.save(
                newAppointment(patientId, request.date(), slot, null));
        publishRequested(saved);
        log.info("Patient {} requested {} at {}", patientId, saved.getDate(), saved.getStartTime());
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurrencePreview previewRecurrence(Long patientId, RecurrenceRequest request) {
        List<LocalDate> available = availableOccurrences(patientId, request);
        return new RecurrencePreview(request.dayOfWeek(), request.startTime(),
                available, unavailableOccurrences(request.dayOfWeek(), available));
    }

    @Override
    public List<AppointmentResponse> requestRecurrence(Long patientId, RecurrenceRequest request) {
        List<LocalDate> available = availableOccurrences(patientId, request);
        if (available.isEmpty()) {
            throw new NoAvailableOccurrencesException(request.dayOfWeek(), request.startTime());
        }

        // Slot length is uniform, so one lookup covers every occurrence
        TimeSlot slot = freeSlot(available.get(0), request.startTime());
        RecurrenceGroup group = recurrenceGroupRepository.save(RecurrenceGroup.builder()
                .patient(patientReference(patientId))
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .build());

        List<Appointment> saved = appointmentRepository.saveAll(available.stream()
                .map(date -> newAppointment(patientId, date, slot, group))
                .toList());
        saved.forEach(this::publishRequested);
        log.info("Patient {} requested {} recurrent occurrences on {} at {}",
                patientId, saved.size(), request.dayOfWeek(), request.startTime());
        return saved.stream().map(appointmentMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> listOwn(Long patientId, Pageable pageable) {
        return appointmentRepository.findByPatientIdOrderByDateDescStartTimeDesc(patientId, pageable)
                .map(appointmentMapper::toResponse);
    }

    @Override
    public void cancel(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new AppointmentAccessDeniedException(appointmentId);
        }
        if (!appointment.getStatus().isLive()) {
            throw new InvalidStatusTransitionException(appointmentId, appointment.getStatus(),
                    AppointmentStatus.LIVE);
        }
        if (!withinCancellationWindow(appointment)) {
            throw new CancellationWindowClosedException(appointmentId);
        }

        appointment.setStatus(AppointmentStatus.CANCELADO);
        log.info("Patient {} cancelled appointment {}", patientId, appointmentId);
    }

    // The patient's own cancellation sends no push: the psychologist has no mobile app
    private boolean withinCancellationWindow(Appointment appointment) {
        LocalDateTime start = appointment.getDate().atTime(appointment.getStartTime());
        return Duration.between(LocalDateTime.now(clock), start).compareTo(CANCELLATION_THRESHOLD) > 0;
    }

    private List<LocalDate> availableOccurrences(Long patientId, RecurrenceRequest request) {
        Set<LocalDate> taken = liveDates(patientId);
        return availabilityService.findAvailableDates(request.dayOfWeek(), request.startTime()).stream()
                .filter(date -> !taken.contains(date))
                .toList();
    }

    private List<LocalDate> unavailableOccurrences(java.time.DayOfWeek dayOfWeek, List<LocalDate> available) {
        return window.start().datesUntil(window.end().plusDays(1))
                .filter(date -> date.getDayOfWeek() == dayOfWeek)
                .filter(date -> !available.contains(date))
                .toList();
    }

    private Set<LocalDate> liveDates(Long patientId) {
        return appointmentRepository.findByPatientIdAndStatusInAndDateBetween(
                        patientId, AppointmentStatus.LIVE, window.start(), window.end()).stream()
                .map(Appointment::getDate)
                .collect(Collectors.toSet());
    }

    // Also yields the end time, so the slot length stays owned by schedule
    private TimeSlot freeSlot(LocalDate date, LocalTime startTime) {
        return availabilityService.getDay(date).slots().stream()
                .filter(slot -> slot.startTime().equals(startTime) && slot.available())
                .findFirst()
                .orElseThrow(() -> new SlotNotAvailableException(date, startTime));
    }

    private void rejectIfDayTaken(Long patientId, LocalDate date) {
        if (appointmentRepository.existsByPatientIdAndDateAndStatusIn(patientId, date, AppointmentStatus.LIVE)) {
            throw new DailyLimitReachedException(date);
        }
    }

    private Appointment newAppointment(Long patientId, LocalDate date, TimeSlot slot, RecurrenceGroup group) {
        return Appointment.builder()
                .patient(patientReference(patientId))
                .recurrenceGroup(group)
                .date(date)
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .status(AppointmentStatus.SOLICITADO)
                .build();
    }

    // The id comes from the authenticated principal, so no lookup is needed
    private User patientReference(Long patientId) {
        return userRepository.getReferenceById(patientId);
    }

    private void publishRequested(Appointment appointment) {
        eventPublisher.publishEvent(new AppointmentRequestedEvent(
                appointment.getId(), appointment.getPatient().getId(),
                appointment.getDate(), appointment.getStartTime()));
    }
}
