package org.clinica.booking.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidaySyncJob {

    private final HolidaySyncService holidaySyncService;
    private final AvailabilityWindow window;

    @Scheduled(cron = "${app.holidays.sync-cron:0 0 4 * * *}")
    public void syncHolidays() {
        IntStream.rangeClosed(window.today().getYear(), window.end().getYear())
                .forEach(this::syncYear);
    }

    private void syncYear(int year) {
        try {
            holidaySyncService.sync(year);
        } catch (RuntimeException ex) {
            log.error("Holiday sync failed for year {}", year, ex);
        }
    }
}
