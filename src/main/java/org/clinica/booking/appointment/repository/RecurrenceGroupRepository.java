package org.clinica.booking.appointment.repository;

import org.clinica.booking.appointment.entity.RecurrenceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurrenceGroupRepository extends JpaRepository<RecurrenceGroup, Long> {
}
