package org.clinica.booking.integration.holidays.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record HolidayDto(
        @JsonProperty("fecha") LocalDate date,
        @JsonProperty("tipo") HolidayType type,
        @JsonProperty("nombre") String name
) {
}