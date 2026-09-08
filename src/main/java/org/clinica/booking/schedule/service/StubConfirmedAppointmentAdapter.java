package org.clinica.booking.schedule.service;

import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// TODO delete when appointment module lands
@Slf4j
@Component
public class StubConfirmedAppointmentAdapter implements ConfirmedAppointmentPort {

    @Override
    public List<ConfirmedSlot> findConfirmed(LocalDate from, LocalDate to) {
        log.debug("Stub port: no appointments between {} and {}", from, to);
        return List.of();
    }

    @Override
    public void cancelConfirmed(List<Long> appointmentIds, String reason) {
        log.warn("Stub port: ignoring cancellation of {}", appointmentIds);
    }

    @Override
    public void rejectPending(LocalDate from, LocalDate to, LocalTime startTime, LocalTime endTime) {
        log.warn("Stub port: ignoring pending rejection between {} {} and {} {}", from, startTime, to, endTime);
    }
}
