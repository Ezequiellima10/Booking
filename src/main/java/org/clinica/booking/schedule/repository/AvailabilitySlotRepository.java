package org.clinica.booking.schedule.repository;

import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findAllByOrderByDayOfWeekAscStartTimeAsc();
}
