package org.clinica.booking.schedule.repository;

import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.SlotType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findAllByOrderByDayOfWeekAscStartTimeAsc();

    List<AvailabilitySlot> findByDayOfWeekAndTypeOrderByStartTimeAsc(DayOfWeek dayOfWeek, SlotType type);

    List<AvailabilitySlot> findByDayOfWeekOrderByStartTimeAsc(DayOfWeek dayOfWeek);
}
