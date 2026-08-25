package org.clinica.booking.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.AvailabilitySlotResponse;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.BlockedDateResponse;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.entity.SlotType;
import org.clinica.booking.schedule.exception.AvailabilitySlotNotFoundException;
import org.clinica.booking.schedule.exception.BlockedDateNotFoundException;
import org.clinica.booking.schedule.exception.ConfirmedAppointmentConflictException;
import org.clinica.booking.schedule.exception.HolidayBlockNotDeletableException;
import org.clinica.booking.schedule.mapper.ScheduleMapper;
import org.clinica.booking.schedule.repository.AvailabilitySlotRepository;
import org.clinica.booking.schedule.repository.BlockedDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduleConfigServiceImpl implements ScheduleConfigService {

    private static final String DEFAULT_CANCELLATION_REASON = "The professional blocked this date";

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final ConfirmedAppointmentPort confirmedAppointmentPort;
    private final ScheduleMapper scheduleMapper;
    private final AvailabilityWindow window;

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilitySlotResponse> listSlots() {
        return currentSlots().stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    public AvailabilitySlotResponse createSlot(AvailabilitySlotRequest request) {
        List<AvailabilitySlot> current = currentSlots();
        AvailabilitySlot candidate = scheduleMapper.toEntity(request);

        List<AvailabilitySlot> resulting = new ArrayList<>(current);
        resulting.add(candidate);
        rejectIfDropsConfirmed(current, resulting);

        AvailabilitySlot saved = availabilitySlotRepository.save(candidate);
        log.info("Availability slot {} created: {} {} {}-{}", saved.getId(), saved.getType(),
                saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime());
        return scheduleMapper.toResponse(saved);
    }

    @Override
    public AvailabilitySlotResponse updateSlot(Long id, AvailabilitySlotRequest request) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(id)
                .orElseThrow(() -> new AvailabilitySlotNotFoundException(id));

        AvailabilitySlot candidate = scheduleMapper.toEntity(request);
        candidate.setId(id);
        List<AvailabilitySlot> current = currentSlots();
        List<AvailabilitySlot> resulting = current.stream()
                .map(existing -> existing.getId().equals(id) ? candidate : existing)
                .toList();
        rejectIfDropsConfirmed(current, resulting);

        scheduleMapper.applyUpdate(slot, request);
        log.info("Availability slot {} updated: {} {} {}-{}", id, slot.getType(),
                slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
        return scheduleMapper.toResponse(slot);
    }

    @Override
    public void deleteSlot(Long id) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(id)
                .orElseThrow(() -> new AvailabilitySlotNotFoundException(id));

        List<AvailabilitySlot> current = currentSlots();
        List<AvailabilitySlot> resulting = current.stream()
                .filter(existing -> !existing.getId().equals(id))
                .toList();
        rejectIfDropsConfirmed(current, resulting);

        availabilitySlotRepository.delete(slot);
        log.info("Availability slot {} deleted", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedDateResponse> listBlocks(LocalDate from, LocalDate to) {
        LocalDate start = from == null ? window.today() : from;
        LocalDate end = to == null ? window.end() : to;
        return blockedDateRepository.findOverlapping(start, end).stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Override
    public BlockedDateResponse createBlock(BlockedDateRequest request) {
        LocalDate from = request.startDate().isBefore(window.today()) ? window.today() : request.startDate();
        List<ConfirmedSlot> conflicts = findConfirmedCoveredBy(request, from);

        if (!conflicts.isEmpty()) {
            if (!request.cancelConfirmed()) {
                throw new ConfirmedAppointmentConflictException(
                        "The block overlaps confirmed appointments", conflicts);
            }
            confirmedAppointmentPort.cancelConfirmed(
                    conflicts.stream().map(ConfirmedSlot::appointmentId).toList(),
                    request.reason() == null ? DEFAULT_CANCELLATION_REASON : request.reason());
            log.info("Cancelled {} confirmed appointments to block {}..{}",
                    conflicts.size(), request.startDate(), request.endDate());
        }

        BlockedDate saved = blockedDateRepository.save(scheduleMapper.toEntity(request));
        if (!from.isAfter(request.endDate())) {
            confirmedAppointmentPort.rejectPending(from, request.endDate());
        }
        log.info("Blocked date {} created: {}..{}", saved.getId(), saved.getStartDate(), saved.getEndDate());
        return scheduleMapper.toResponse(saved);
    }

    @Override
    public void deleteBlock(Long id) {
        BlockedDate block = blockedDateRepository.findById(id)
                .orElseThrow(() -> new BlockedDateNotFoundException(id));
        if (block.getSource() == BlockSource.HOLIDAY_API) {
            throw new HolidayBlockNotDeletableException(id);
        }
        blockedDateRepository.delete(block);
        log.info("Blocked date {} deleted", id);
    }

    private List<AvailabilitySlot> currentSlots() {
        return availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
    }

    private List<ConfirmedSlot> findConfirmedCoveredBy(BlockedDateRequest request, LocalDate from) {
        if (from.isAfter(request.endDate())) {
            return List.of();
        }
        List<ConfirmedSlot> confirmed = confirmedAppointmentPort.findConfirmed(from, request.endDate());
        if (request.startTime() == null) {
            return confirmed;
        }
        return confirmed.stream()
                .filter(slot -> overlaps(slot.startTime(), slot.endTime(), request.startTime(), request.endTime()))
                .toList();
    }

    private void rejectIfDropsConfirmed(List<AvailabilitySlot> current, List<AvailabilitySlot> resulting) {
        List<ConfirmedSlot> confirmed = confirmedAppointmentPort.findConfirmed(window.today(), window.end());
        if (confirmed.isEmpty()) {
            return;
        }
        Map<DayOfWeek, List<AvailabilitySlot>> before = groupByDayOfWeek(current);
        Map<DayOfWeek, List<AvailabilitySlot>> after = groupByDayOfWeek(resulting);

        List<ConfirmedSlot> conflicts = confirmed.stream()
                .filter(slot -> fits(slot, before) && !fits(slot, after))
                .toList();
        if (!conflicts.isEmpty()) {
            throw new ConfirmedAppointmentConflictException(
                    "The change would leave confirmed appointments outside working hours", conflicts);
        }
    }

    private Map<DayOfWeek, List<AvailabilitySlot>> groupByDayOfWeek(List<AvailabilitySlot> slots) {
        return slots.stream().collect(Collectors.groupingBy(AvailabilitySlot::getDayOfWeek));
    }

    private boolean fits(ConfirmedSlot slot, Map<DayOfWeek, List<AvailabilitySlot>> byDayOfWeek) {
        List<AvailabilitySlot> daySlots = byDayOfWeek.getOrDefault(slot.date().getDayOfWeek(), List.of());
        boolean insideWorkingRange = daySlots.stream()
                .filter(s -> s.getType() == SlotType.WORKING_RANGE)
                .anyMatch(s -> !slot.startTime().isBefore(s.getStartTime())
                        && !slot.endTime().isAfter(s.getEndTime()));
        return insideWorkingRange && daySlots.stream()
                .filter(s -> s.getType() == SlotType.BREAK)
                .noneMatch(s -> overlaps(slot.startTime(), slot.endTime(), s.getStartTime(), s.getEndTime()));
    }

    private boolean overlaps(LocalTime start, LocalTime end, LocalTime otherStart, LocalTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
