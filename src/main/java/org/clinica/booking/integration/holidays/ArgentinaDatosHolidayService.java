package org.clinica.booking.integration.holidays;

import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.config.HolidayApiConfig;
import org.clinica.booking.integration.holidays.dto.HolidayDto;
import org.clinica.booking.integration.holidays.exception.HolidayProviderException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@Slf4j
public class ArgentinaDatosHolidayService implements HolidayProviderService {

    private final RestClient restClient;

    public ArgentinaDatosHolidayService(RestClient.Builder restClientBuilder, HolidayApiConfig config) {
        this.restClient = restClientBuilder.baseUrl(config.baseUrl()).build();
    }

    @Override
    public List<HolidayDto> getHolidays(int year) {
        try {
            List<HolidayDto> holidays = restClient.get()
                    .uri("/v1/feriados/{year}", year)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<HolidayDto>>() {
                    });
            log.info("Fetched {} feriados for year {} from ArgentinaDatos", holidays == null ? 0 : holidays.size(), year);
            return holidays == null ? List.of() : holidays;
        } catch (RestClientException ex) {
            throw new HolidayProviderException(year, ex);
        }
    }
}
