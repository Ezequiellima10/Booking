package org.clinica.booking.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class AvailabilityWindow {

    private static final int DAYS_AHEAD = 31;

    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDate start() {
        return today().plusDays(1);
    }

    public LocalDate end() {
        return today().plusDays(DAYS_AHEAD);
    }

    public boolean contains(LocalDate date) {
        return date != null && !date.isBefore(start()) && !date.isAfter(end());
    }

    public LocalDate clampStart(LocalDate from) {
        return from == null || from.isBefore(start()) ? start() : from;
    }

    public LocalDate clampEnd(LocalDate to) {
        return to == null || to.isAfter(end()) ? end() : to;
    }
}
