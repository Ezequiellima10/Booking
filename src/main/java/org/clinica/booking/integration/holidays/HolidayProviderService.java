package org.clinica.booking.integration.holidays;

import org.clinica.booking.integration.holidays.dto.HolidayDto;

import java.util.List;

public interface HolidayProviderService {

    List<HolidayDto> getHolidays(int year);
}