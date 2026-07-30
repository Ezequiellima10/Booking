package org.clinica.booking.integration.holidays.exception;

public class HolidayProviderException extends RuntimeException {

    public HolidayProviderException(int year, Throwable cause) {
        super("Failed to fetch holidays for year " + year, cause);
    }
}
