package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.HolidayConflict;

import java.util.List;

public interface HolidaySyncService {

    void sync(int year);

    List<HolidayConflict> findHolidayConflicts();
}
