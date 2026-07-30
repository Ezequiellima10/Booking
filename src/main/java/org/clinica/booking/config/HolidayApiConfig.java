package org.clinica.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.holidays")
public record HolidayApiConfig(
        String baseUrl
) {
}