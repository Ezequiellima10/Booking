package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.dto.DayAvailability;
import org.clinica.booking.schedule.dto.DaySchedule;
import org.clinica.booking.schedule.dto.TimeSlot;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.entity.SlotType;
import org.clinica.booking.schedule.repository.AvailabilitySlotRepository;
import org.clinica.booking.schedule.repository.BlockedDateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 25);
    private static final LocalDate WINDOW_END = LocalDate.of(2026, 9, 24);

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Mock
    private BlockedDateRepository blockedDateRepository;

    @Mock
    private ConfirmedAppointmentPort confirmedAppointmentPort;

    private AvailabilityServiceImpl availabilityService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        availabilityService = new AvailabilityServiceImpl(availabilitySlotRepository, blockedDateRepository,
                confirmedAppointmentPort, new AvailabilityWindow(clock));
    }

    private AvailabilitySlot slot(SlotType type, DayOfWeek dayOfWeek, String start, String end) {
        return AvailabilitySlot.builder()
                .id(1L)
                .type(type)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private BlockedDate block(LocalDate date, String start, String end) {
        return BlockedDate.builder()
                .id(1L)
                .startDate(date)
                .endDate(date)
                .startTime(start == null ? null : LocalTime.parse(start))
                .endTime(end == null ? null : LocalTime.parse(end))
                .source(BlockSource.MANUAL)
                .build();
    }

    private ConfirmedSlot confirmed(LocalDate date, String start) {
        LocalTime startTime = LocalTime.parse(start);
        return new ConfirmedSlot(7L, date, startTime, startTime.plusHours(1), "Ana Gomez");
    }

    private void stubSchedule(List<AvailabilitySlot> slots, List<BlockedDate> blocks, List<ConfirmedSlot> confirmed) {
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()).thenReturn(slots);
        when(blockedDateRepository.findOverlapping(any(), any())).thenReturn(blocks);
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(confirmed);
    }

    @Test
    void getDay_generatesHourlySlotsFromWorkingRanges() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(), List.of());

        DaySchedule schedule = availabilityService.getDay(TUESDAY);

        assertThat(schedule.slots()).extracting(TimeSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0));
        assertThat(schedule.slots()).allMatch(TimeSlot::available);
    }

    @Test
    void getDay_discardsPartialTrailingHour() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "11:30")),
                List.of(), List.of());

        assertThat(availabilityService.getDay(TUESDAY).slots()).extracting(TimeSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    void getDay_deduplicatesOverlappingWorkingRanges() {
        stubSchedule(List.of(
                        slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "12:00"),
                        slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "11:00", "13:00")),
                List.of(), List.of());

        assertThat(availabilityService.getDay(TUESDAY).slots()).extracting(TimeSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0));
    }

    @Test
    void getDay_keepsBreakSlotsVisibleButUnavailable() {
        stubSchedule(List.of(
                        slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00"),
                        slot(SlotType.BREAK, DayOfWeek.TUESDAY, "12:00", "13:00")),
                List.of(), List.of());

        List<TimeSlot> slots = availabilityService.getDay(TUESDAY).slots();

        assertThat(slots).hasSize(4);
        assertThat(slots.get(3).startTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(slots.get(3).available()).isFalse();
    }

    @Test
    void getDay_marksEverySlotUnavailableOnFullDayBlock() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(block(TUESDAY, null, null)), List.of());

        assertThat(availabilityService.getDay(TUESDAY).slots()).noneMatch(TimeSlot::available);
    }

    @Test
    void getDay_marksOnlyOverlappedSlotsOnPartialBlock() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(block(TUESDAY, "10:00", "11:00")), List.of());

        List<TimeSlot> slots = availabilityService.getDay(TUESDAY).slots();

        assertThat(slots).filteredOn(TimeSlot::available).extracting(TimeSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0), LocalTime.of(12, 0));
    }

    @Test
    void getDay_discountsConfirmedAppointments() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(), List.of(confirmed(TUESDAY, "11:00")));

        List<TimeSlot> slots = availabilityService.getDay(TUESDAY).slots();

        assertThat(slots).filteredOn(TimeSlot::available).extracting(TimeSlot::startTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(12, 0));
    }

    @Test
    void getDay_returnsNoSlotsOutsideTheWindow() {
        assertThat(availabilityService.getDay(TODAY).slots()).isEmpty();
        assertThat(availabilityService.getDay(WINDOW_END.plusDays(1)).slots()).isEmpty();
    }

    @Test
    void getRange_clampsToTheWindowInsteadOfFailing() {
        stubSchedule(List.of(), List.of(), List.of());

        List<DayAvailability> range = availabilityService.getRange(
                LocalDate.of(2020, 1, 1), LocalDate.of(2030, 1, 1));

        assertThat(range).hasSize(31);
        assertThat(range.get(0).date()).isEqualTo(TUESDAY);
        assertThat(range.get(30).date()).isEqualTo(WINDOW_END);
    }

    @Test
    void getRange_returnsEmptyForRangesEntirelyInThePast() {
        assertThat(availabilityService.getRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1))).isEmpty();
    }

    @Test
    void getRange_countsOnlyFreeSlotsPerDay() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(), List.of(confirmed(TUESDAY, "09:00")));

        List<DayAvailability> range = availabilityService.getRange(TUESDAY, TUESDAY.plusDays(1));

        assertThat(range).containsExactly(
                new DayAvailability(TUESDAY, 3),
                new DayAvailability(TUESDAY.plusDays(1), 0));
    }

    @Test
    void isSlotAvailable_reflectsConfirmedAppointments() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(), List.of(confirmed(TUESDAY, "09:00")));

        assertThat(availabilityService.isSlotAvailable(TUESDAY, LocalTime.of(9, 0))).isFalse();
        assertThat(availabilityService.isSlotAvailable(TUESDAY, LocalTime.of(10, 0))).isTrue();
    }

    @Test
    void findAvailableDates_returnsMatchingWeekdaysInsideTheWindow() {
        stubSchedule(List.of(slot(SlotType.WORKING_RANGE, DayOfWeek.TUESDAY, "09:00", "13:00")),
                List.of(block(LocalDate.of(2026, 9, 1), null, null)), List.of());

        List<LocalDate> dates = availabilityService.findAvailableDates(DayOfWeek.TUESDAY, LocalTime.of(9, 0));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 9, 8),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 9, 22));
    }
}
