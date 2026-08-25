package org.clinica.booking.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.integration.holidays.HolidayProviderService;
import org.clinica.booking.integration.holidays.dto.HolidayDto;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.dto.HolidayConflict;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.repository.BlockedDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidaySyncServiceImpl implements HolidaySyncService {

    private final HolidayProviderService holidayProviderService;
    private final BlockedDateRepository blockedDateRepository;
    private final ConfirmedAppointmentPort confirmedAppointmentPort;
    private final AvailabilityWindow window;

    @Override
    public void sync(int year) {
        List<HolidayDto> holidays = holidayProviderService.getHolidays(year);
        Set<LocalDate> alreadyBlocked = blockedDatesFor(year);
        Map<LocalDate, List<ConfirmedSlot>> confirmedByDate = confirmedWithinWindow();

        List<BlockedDate> toInsert = holidays.stream()
                .filter(holiday -> !holiday.date().isBefore(window.today()))
                .filter(holiday -> !alreadyBlocked.contains(holiday.date()))
                .filter(holiday -> !hasConfirmed(confirmedByDate, holiday))
                .map(this::toBlockedDate)
                .toList();

        if (toInsert.isEmpty()) {
            log.info("Holiday sync for {}: nothing to insert", year);
            return;
        }
        blockedDateRepository.saveAll(toInsert);
        log.info("Holiday sync for {}: inserted {} blocks", year, toInsert.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayConflict> findHolidayConflicts() {
        LocalDate from = window.start();
        LocalDate to = window.end();
        Map<LocalDate, List<ConfirmedSlot>> confirmedByDate = confirmedWithinWindow();
        if (confirmedByDate.isEmpty()) {
            return List.of();
        }
        Set<LocalDate> alreadyBlocked = blockedDateRepository
                .findBySourceAndStartDateBetween(BlockSource.HOLIDAY_API, from, to).stream()
                .map(BlockedDate::getStartDate)
                .collect(Collectors.toSet());

        return holidaysBetween(from, to).stream()
                .filter(holiday -> !alreadyBlocked.contains(holiday.date()))
                .filter(holiday -> confirmedByDate.containsKey(holiday.date()))
                .map(holiday -> new HolidayConflict(holiday.date(), holiday.name(),
                        confirmedByDate.get(holiday.date())))
                .toList();
    }

    private boolean hasConfirmed(Map<LocalDate, List<ConfirmedSlot>> confirmedByDate, HolidayDto holiday) {
        if (!confirmedByDate.containsKey(holiday.date())) {
            return false;
        }
        log.warn("Holiday {} on {} skipped: a confirmed appointment already covers that date",
                holiday.name(), holiday.date());
        return true;
    }

    private Map<LocalDate, List<ConfirmedSlot>> confirmedWithinWindow() {
        return confirmedAppointmentPort.findConfirmed(window.today(), window.end()).stream()
                .collect(Collectors.groupingBy(ConfirmedSlot::date));
    }

    private Set<LocalDate> blockedDatesFor(int year) {
        return blockedDateRepository.findBySourceAndStartDateBetween(
                        BlockSource.HOLIDAY_API, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .map(BlockedDate::getStartDate)
                .collect(Collectors.toSet());
    }

    private List<HolidayDto> holidaysBetween(LocalDate from, LocalDate to) {
        return IntStream.rangeClosed(from.getYear(), to.getYear())
                .boxed()
                .flatMap(year -> holidayProviderService.getHolidays(year).stream())
                .filter(holiday -> !holiday.date().isBefore(from) && !holiday.date().isAfter(to))
                .toList();
    }

    private BlockedDate toBlockedDate(HolidayDto holiday) {
        return BlockedDate.builder()
                .startDate(holiday.date())
                .endDate(holiday.date())
                .reason(holiday.name())
                .source(BlockSource.HOLIDAY_API)
                .build();
    }
}
