package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.clinica.booking.schedule.entity.SlotType;
import org.clinica.booking.schedule.exception.AvailabilitySlotNotFoundException;
import org.clinica.booking.schedule.exception.ConfirmedAppointmentConflictException;
import org.clinica.booking.schedule.exception.HolidayBlockNotDeletableException;
import org.clinica.booking.schedule.mapper.ScheduleMapper;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleConfigServiceImplTest {

    private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 25);

    @Mock
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Mock
    private BlockedDateRepository blockedDateRepository;

    @Mock
    private ConfirmedAppointmentPort confirmedAppointmentPort;

    private ScheduleConfigServiceImpl scheduleConfigService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        scheduleConfigService = new ScheduleConfigServiceImpl(availabilitySlotRepository, blockedDateRepository,
                confirmedAppointmentPort, new ScheduleMapper(), new AvailabilityWindow(clock));
    }

    private AvailabilitySlot workingRange(Long id, String start, String end) {
        return AvailabilitySlot.builder()
                .id(id)
                .type(SlotType.WORKING_RANGE)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }

    private AvailabilitySlotRequest slotRequest(SlotType type, String start, String end) {
        return new AvailabilitySlotRequest(type, DayOfWeek.TUESDAY,
                LocalTime.parse(start), LocalTime.parse(end), null);
    }

    private ConfirmedSlot confirmedAtNoon() {
        return new ConfirmedSlot(7L, TUESDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "Ana Gomez");
    }

    private BlockedDateRequest fullDayBlock(boolean cancelConfirmed) {
        return new BlockedDateRequest(TUESDAY, TUESDAY, null, null, "Vacaciones", cancelConfirmed);
    }

    @Test
    void updateSlot_rejectsWhenTheChangeLeavesAConfirmedOutside() {
        AvailabilitySlot existing = workingRange(1L, "09:00", "13:00");
        when(availabilitySlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()).thenReturn(List.of(existing));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));

        assertThatThrownBy(() -> scheduleConfigService.updateSlot(1L, slotRequest(SlotType.WORKING_RANGE, "09:00", "11:00")))
                .isInstanceOf(ConfirmedAppointmentConflictException.class)
                .extracting(ex -> ((ConfirmedAppointmentConflictException) ex).getConflicts())
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.list(ConfirmedSlot.class))
                .containsExactly(confirmedAtNoon());

        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void updateSlot_allowsChangesThatKeepConfirmedInside() {
        AvailabilitySlot existing = workingRange(1L, "09:00", "13:00");
        when(availabilitySlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()).thenReturn(List.of(existing));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));

        scheduleConfigService.updateSlot(1L, slotRequest(SlotType.WORKING_RANGE, "09:00", "20:00"));

        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    void createSlot_rejectsABreakLandingOnAConfirmedAppointment() {
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc())
                .thenReturn(List.of(workingRange(1L, "09:00", "13:00")));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));

        assertThatThrownBy(() -> scheduleConfigService.createSlot(slotRequest(SlotType.BREAK, "12:00", "13:00")))
                .isInstanceOf(ConfirmedAppointmentConflictException.class);

        verify(availabilitySlotRepository, never()).save(any());
    }

    @Test
    void createSlot_allowsABreakOutsideConfirmedAppointments() {
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc())
                .thenReturn(List.of(workingRange(1L, "09:00", "13:00")));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));
        when(availabilitySlotRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(scheduleConfigService.createSlot(slotRequest(SlotType.BREAK, "09:00", "10:00")).type())
                .isEqualTo(SlotType.BREAK);
    }

    @Test
    void deleteSlot_rejectsWhenAConfirmedFallsInsideTheDeletedRange() {
        AvailabilitySlot existing = workingRange(1L, "09:00", "13:00");
        when(availabilitySlotRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(availabilitySlotRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()).thenReturn(List.of(existing));
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));

        assertThatThrownBy(() -> scheduleConfigService.deleteSlot(1L))
                .isInstanceOf(ConfirmedAppointmentConflictException.class);

        verify(availabilitySlotRepository, never()).delete(any());
    }

    @Test
    void updateSlot_throwsWhenTheSlotDoesNotExist() {
        when(availabilitySlotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleConfigService.updateSlot(99L, slotRequest(SlotType.BREAK, "09:00", "10:00")))
                .isInstanceOf(AvailabilitySlotNotFoundException.class);
    }

    @Test
    void createBlock_rejectsWhenAConfirmedAppointmentOverlaps() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));

        assertThatThrownBy(() -> scheduleConfigService.createBlock(fullDayBlock(false)))
                .isInstanceOf(ConfirmedAppointmentConflictException.class);

        verify(blockedDateRepository, never()).save(any());
        verify(confirmedAppointmentPort, never()).cancelConfirmed(any(), any());
    }

    @Test
    void createBlock_cancelsConfirmedWhenTheFlagIsSet() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));
        when(blockedDateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        scheduleConfigService.createBlock(fullDayBlock(true));

        verify(confirmedAppointmentPort).cancelConfirmed(List.of(7L), "Vacaciones");
        verify(blockedDateRepository).save(any());
        verify(confirmedAppointmentPort).rejectPending(TUESDAY, TUESDAY, null, null);
    }

    @Test
    void createBlock_ignoresConfirmedOutsideThePartialBlockHours() {
        when(confirmedAppointmentPort.findConfirmed(any(), any())).thenReturn(List.of(confirmedAtNoon()));
        when(blockedDateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        BlockedDateRequest request = new BlockedDateRequest(TUESDAY, TUESDAY,
                LocalTime.of(15, 0), LocalTime.of(16, 0), "Tramite", false);

        assertThat(scheduleConfigService.createBlock(request).source()).isEqualTo(BlockSource.MANUAL);
        verify(confirmedAppointmentPort, never()).cancelConfirmed(any(), any());
    }

    @Test
    void createBlock_rejectsPendingOnlyWithinThePartialBlockHours() {
        when(blockedDateRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        BlockedDateRequest request = new BlockedDateRequest(TUESDAY, TUESDAY,
                LocalTime.of(15, 0), LocalTime.of(16, 0), "Tramite", false);
        scheduleConfigService.createBlock(request);

        verify(confirmedAppointmentPort).rejectPending(TUESDAY, TUESDAY, LocalTime.of(15, 0), LocalTime.of(16, 0));
    }

    @Test
    void deleteBlock_rejectsHolidayBlocks() {
        BlockedDate holiday = BlockedDate.builder()
                .id(5L).startDate(TUESDAY).endDate(TUESDAY).source(BlockSource.HOLIDAY_API).build();
        when(blockedDateRepository.findById(5L)).thenReturn(Optional.of(holiday));

        assertThatThrownBy(() -> scheduleConfigService.deleteBlock(5L))
                .isInstanceOf(HolidayBlockNotDeletableException.class);

        verify(blockedDateRepository, never()).delete(any());
    }

    @Test
    void deleteBlock_removesManualBlocks() {
        BlockedDate manual = BlockedDate.builder()
                .id(6L).startDate(TUESDAY).endDate(TUESDAY).source(BlockSource.MANUAL).build();
        when(blockedDateRepository.findById(6L)).thenReturn(Optional.of(manual));

        scheduleConfigService.deleteBlock(6L);

        verify(blockedDateRepository).delete(manual);
    }
}
