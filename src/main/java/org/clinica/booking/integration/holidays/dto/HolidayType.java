package org.clinica.booking.integration.holidays.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HolidayType {

    @JsonProperty("inamovible")
    INAMOVIBLE,

    @JsonProperty("trasladable")
    TRASLADABLE,

    @JsonProperty("puente")
    PUENTE
}
