package org.clinica.booking.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.dto.DayAvailability;
import org.clinica.booking.schedule.dto.DaySchedule;
import org.clinica.booking.schedule.dto.TimeSlot;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.entity.SlotType;
import org.clinica.booking.schedule.repository.AvailabilitySlotRepository;
import org.clinica.booking.schedule.repository.BlockedDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Duration SLOT_LENGTH = Duration.ofHours(1);

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final ConfirmedAppointmentPort confirmedAppointmentPort;
    private final AvailabilityWindow window;

    @Override
    public List<DayAvailability> getRange(LocalDate from, LocalDate to) {
        LocalDate start = window.clampStart(from);
        LocalDate end = window.clampEnd(to);
        if (start.isAfter(end)) {
            log.debug("Range {}..{} falls outside the availability window", from, to);
            return List.of();
        }
        ScheduleSnapshot snapshot = loadSnapshot(start, end);
        return start.datesUntil(end.plusDays(1))
                .map(date -> new DayAvailability(date, countFree(computeDay(date, snapshot))))
                .toList();
    }

    @Override
    public DaySchedule getDay(LocalDate date) {
        if (!window.contains(date)) {
            log.debug("Day {} falls outside the availability window", date);
            return new DaySchedule(date, List.of());
        }
        return new DaySchedule(date, computeDay(date, loadSnapshot(date, date)));
    }

    @Override
    public List<LocalDate> findAvailableDates(DayOfWeek dayOfWeek, LocalTime startTime) {
        LocalDate start = window.start();
        LocalDate end = window.end();
        ScheduleSnapshot snapshot = loadSnapshot(start, end);
        return start.datesUntil(end.plusDays(1))
                .filter(date -> date.getDayOfWeek() == dayOfWeek)
                .filter(date -> computeDay(date, snapshot).stream()
                        .anyMatch(slot -> slot.startTime().equals(startTime) && slot.available()))
                .toList();
    }

    private ScheduleSnapshot loadSnapshot(LocalDate from, LocalDate to) {
        Map<DayOfWeek, List<AvailabilitySlot>> slotsByDay =
                availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                        .collect(Collectors.groupingBy(AvailabilitySlot::getDayOfWeek));
        List<BlockedDate> blocks = blockedDateRepository.findOverlapping(from, to);
        Map<LocalDate, List<ConfirmedSlot>> confirmedByDate =
                confirmedAppointmentPort.findConfirmed(from, to).stream()
                        .collect(Collectors.groupingBy(ConfirmedSlot::date));
        return new ScheduleSnapshot(slotsByDay, blocks, confirmedByDate);
    }

    private List<TimeSlot> computeDay(LocalDate date, ScheduleSnapshot snapshot) {
        List<AvailabilitySlot> daySlots = snapshot.slotsByDay().getOrDefault(date.getDayOfWeek(), List.of());

        SortedSet<LocalTime> starts = new TreeSet<>();
        daySlots.stream()
                .filter(slot -> slot.getType() == SlotType.WORKING_RANGE)
                .forEach(slot -> collectHourlyStarts(starts, slot.getStartTime(), slot.getEndTime()));
        if (starts.isEmpty()) {
            return List.of();
        }

        List<AvailabilitySlot> breaks = daySlots.stream()
                .filter(slot -> slot.getType() == SlotType.BREAK)
                .toList();
        List<BlockedDate> dayBlocks = snapshot.blocks().stream()
                .filter(block -> covers(block, date))
                .toList();
        List<ConfirmedSlot> confirmed = snapshot.confirmedByDate().getOrDefault(date, List.of());

        return starts.stream()
                .map(start -> new TimeSlot(start, start.plus(SLOT_LENGTH),
                        isFree(start, breaks, dayBlocks, confirmed)))
                .toList();
    }

    private void collectHourlyStarts(SortedSet<LocalTime> starts, LocalTime rangeStart, LocalTime rangeEnd) {
        for (LocalTime start = rangeStart; ; start = start.plus(SLOT_LENGTH)) {
            LocalTime end = start.plus(SLOT_LENGTH);
            if (!end.isAfter(start) || end.isAfter(rangeEnd)) {
                return;
            }
            starts.add(start);
        }
    }

    private boolean isFree(LocalTime start, List<AvailabilitySlot> breaks, List<BlockedDate> blocks,
                           List<ConfirmedSlot> confirmed) {
        LocalTime end = start.plus(SLOT_LENGTH);
        if (breaks.stream().anyMatch(b -> overlaps(start, end, b.getStartTime(), b.getEndTime()))) {
            return false;
        }
        if (blocks.stream().anyMatch(b -> b.isFullDay() || overlaps(start, end, b.getStartTime(), b.getEndTime()))) {
            return false;
        }
        return confirmed.stream().noneMatch(c -> overlaps(start, end, c.startTime(), c.endTime()));
    }

    private boolean covers(BlockedDate block, LocalDate date) {
        return !date.isBefore(block.getStartDate()) && !date.isAfter(block.getEndDate());
    }

    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    private int countFree(List<TimeSlot> slots) {
        return (int) slots.stream().filter(TimeSlot::available).count();
    }

    private record ScheduleSnapshot(
            Map<DayOfWeek, List<AvailabilitySlot>> slotsByDay,
            List<BlockedDate> blocks,
            Map<LocalDate, List<ConfirmedSlot>> confirmedByDate) {
    }
}
