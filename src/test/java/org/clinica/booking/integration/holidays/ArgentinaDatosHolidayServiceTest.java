package org.clinica.booking.integration.holidays;

import org.clinica.booking.config.HolidayApiConfig;
import org.clinica.booking.integration.holidays.dto.HolidayDto;
import org.clinica.booking.integration.holidays.dto.HolidayType;
import org.clinica.booking.integration.holidays.exception.HolidayProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ArgentinaDatosHolidayServiceTest {

    private static final HolidayApiConfig CONFIG = new HolidayApiConfig("https://api.argentinadatos.com");

    private MockRestServiceServer mockServer;
    private ArgentinaDatosHolidayService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new ArgentinaDatosHolidayService(builder, CONFIG);
    }

    @Test
    void getHolidays_mapsFechaTipoNombre_toHolidayDto() {
        mockServer.expect(requestTo("https://api.argentinadatos.com/v1/feriados/2026"))
                .andRespond(withSuccess("""
                        [
                          {"fecha":"2026-01-01","tipo":"inamovible","nombre":"Año nuevo"},
                          {"fecha":"2026-03-23","tipo":"puente","nombre":"Puente turístico no laborable"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<HolidayDto> holidays = service.getHolidays(2026);

        assertThat(holidays).containsExactly(
                new HolidayDto(LocalDate.of(2026, 1, 1), HolidayType.INAMOVIBLE, "Año nuevo"),
                new HolidayDto(LocalDate.of(2026, 3, 23), HolidayType.PUENTE, "Puente turístico no laborable"));
    }

    @Test
    void getHolidays_apiFails_throwsHolidayProviderException() {
        mockServer.expect(requestTo("https://api.argentinadatos.com/v1/feriados/2026"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.getHolidays(2026))
                .isInstanceOf(HolidayProviderException.class)
                .hasMessageContaining("2026");
    }
}
