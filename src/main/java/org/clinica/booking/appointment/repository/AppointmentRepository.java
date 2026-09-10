package org.clinica.booking.appointment.repository;

import org.clinica.booking.appointment.entity.Appointment;
import org.clinica.booking.appointment.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            WHERE a.status = :status AND a.date BETWEEN :from AND :to
            ORDER BY a.date ASC, a.startTime ASC
            """)
    List<Appointment> findWithPatientByStatusAndDateBetween(@Param("status") AppointmentStatus status,
                                                            @Param("from") LocalDate from,
                                                            @Param("to") LocalDate to);

    // FIFO inside each slot
    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.patient
            WHERE a.status IN :statuses AND a.date BETWEEN :from AND :to
            ORDER BY a.date ASC, a.startTime ASC, a.createdAt ASC
            """)
    List<Appointment> findWithPatientByStatusInAndDateBetween(
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<Appointment> findByDateAndStartTimeAndStatusIn(LocalDate date, LocalTime startTime,
                                                        Collection<AppointmentStatus> statuses);

    List<Appointment> findByRecurrenceGroupIdAndStatusIn(Long recurrenceGroupId,
                                                         Collection<AppointmentStatus> statuses);

    List<Appointment> findByPatientIdAndStatusInAndDateBetween(Long patientId,
                                                               Collection<AppointmentStatus> statuses,
                                                               LocalDate from, LocalDate to);

    boolean existsByPatientIdAndDateAndStatusIn(Long patientId, LocalDate date,
                                                Collection<AppointmentStatus> statuses);

    Page<Appointment> findByPatientIdOrderByDateDescStartTimeDesc(Long patientId, Pageable pageable);
}
