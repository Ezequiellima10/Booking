package org.clinica.booking.schedule.service;

import org.clinica.booking.integration.holidays.HolidayProviderService;
import org.clinica.booking.integration.holidays.dto.HolidayDto;
import org.clinica.booking.integration.holidays.dto.HolidayType;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.dto.HolidayConflict;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.repository.BlockedDateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidaySyncServiceImplTest {

    private static final LocalDate PAST_HOLIDAY = LocalDate.of(2026, 5, 25);
    private static final LocalDate IN_WINDOW = LocalDate.of(2026, 9, 1);
    private static final LocalDate BEYOND_WINDOW = LocalDate.of(2026, 12, 8);

    @Mock
    private HolidayProviderService holidayProviderService;

    @Mock
    private BlockedDateRepository blockedDateRepository;

    @Mock
    private ConfirmedAppointmentPort confirmedAppointmentPort;

    @Captor
    private ArgumentCaptor<List<BlockedDate>> insertedCaptor;

    private HolidaySyncServiceImpl holidaySyncService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        holidaySyncService = new HolidaySyncServiceImpl(holidayProviderService, blockedDateRepository,
                confirmedAppointmentPort, new AvailabilityWindow(clock));
    }

    private HolidayDto holiday(LocalDate date, String name) {
        return new HolidayDto(date, HolidayType.INAMOVIBLE, name);
    }

    private BlockedDate holidayBlock(LocalDate date) {
        return BlockedDate.builder()
                .id(1L).startDate(date).endDate(date).source(BlockSource.HOLIDAY_API).build();
    }

    private ConfirmedSlot confirmedOn(LocalDate date) {
        return new ConfirmedSlot(7L, date, LocalTime.of(12, 0), LocalTime.of(13, 0), "Ana Gomez");
    }

    @Test
    void sync_insertsFutureHolidaysAsFullDayBlocks() {
        when(holidayProviderService.getHolidays(2026)).thenReturn(
                List.of(holiday(IN_WINDOW, "Feriado puente"), holiday(BEYOND_WINDOW, "Inmaculada Concepcion")));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any())).thenReturn(List.of());
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of());

        holidaySyncService.sync(2026);

        verify(blockedDateRepository).saveAll(insertedCaptor.capture());
        assertThat(insertedCaptor.getValue())
                .extracting(BlockedDate::getStartDate, BlockedDate::getEndDate,
                        BlockedDate::getStartTime, BlockedDate::getSource)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(IN_WINDOW, IN_WINDOW, null, BlockSource.HOLIDAY_API),
                        org.assertj.core.groups.Tuple.tuple(BEYOND_WINDOW, BEYOND_WINDOW, null, BlockSource.HOLIDAY_API));
    }

    @Test
    void sync_skipsPastHolidays() {
        when(holidayProviderService.getHolidays(2026)).thenReturn(List.of(holiday(PAST_HOLIDAY, "Revolucion de Mayo")));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any())).thenReturn(List.of());
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of());

        holidaySyncService.sync(2026);

        verify(blockedDateRepository, never()).saveAll(any());
    }

    @Test
    void sync_isIdempotentForAlreadyBlockedHolidays() {
        when(holidayProviderService.getHolidays(2026)).thenReturn(List.of(holiday(IN_WINDOW, "Feriado puente")));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any()))
                .thenReturn(List.of(holidayBlock(IN_WINDOW)));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of());

        holidaySyncService.sync(2026);

        verify(blockedDateRepository, never()).saveAll(any());
    }

    @Test
    void sync_skipsHolidaysCoveredByAConfirmedAppointment() {
        when(holidayProviderService.getHolidays(2026)).thenReturn(List.of(holiday(IN_WINDOW, "Feriado puente")));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any())).thenReturn(List.of());
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedOn(IN_WINDOW)));

        holidaySyncService.sync(2026);

        verify(blockedDateRepository, never()).saveAll(any());
    }

    @Test
    void findHolidayConflicts_returnsSkippedHolidaysWithTheirAppointments() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedOn(IN_WINDOW)));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any())).thenReturn(List.of());
        when(holidayProviderService.getHolidays(2026)).thenReturn(
                List.of(holiday(IN_WINDOW, "Feriado puente"), holiday(BEYOND_WINDOW, "Inmaculada Concepcion")));

        List<HolidayConflict> conflicts = holidaySyncService.findHolidayConflicts();

        assertThat(conflicts).containsExactly(
                new HolidayConflict(IN_WINDOW, "Feriado puente", List.of(confirmedOn(IN_WINDOW))));
    }

    @Test
    void findHolidayConflicts_dropsHolidaysAlreadyBlocked() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedOn(IN_WINDOW)));
        when(blockedDateRepository.findBySourceAndStartDateBetween(any(), any(), any()))
                .thenReturn(List.of(holidayBlock(IN_WINDOW)));
        when(holidayProviderService.getHolidays(2026)).thenReturn(List.of(holiday(IN_WINDOW, "Feriado puente")));

        assertThat(holidaySyncService.findHolidayConflicts()).isEmpty();
    }

    @Test
    void findHolidayConflicts_skipsTheProviderWhenThereAreNoConfirmedAppointments() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of());

        assertThat(holidaySyncService.findHolidayConflicts()).isEmpty();
        verifyNoInteractions(holidayProviderService);
    }
}
