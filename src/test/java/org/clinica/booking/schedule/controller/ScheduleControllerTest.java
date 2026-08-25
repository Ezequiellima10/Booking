package org.clinica.booking.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.auth.service.JwtService;
import org.clinica.booking.config.SecurityConfig;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.ConfirmedSlot;
import org.clinica.booking.schedule.dto.DayAvailability;
import org.clinica.booking.schedule.dto.DaySchedule;
import org.clinica.booking.schedule.dto.TimeSlot;
import org.clinica.booking.schedule.exception.ConfirmedAppointmentConflictException;
import org.clinica.booking.schedule.exception.HolidayBlockNotDeletableException;
import org.clinica.booking.schedule.service.AvailabilityService;
import org.clinica.booking.schedule.service.HolidaySyncService;
import org.clinica.booking.schedule.service.ScheduleConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AvailabilityController.class, ScheduleConfigController.class})
@Import(SecurityConfig.class)
class ScheduleControllerTest {

    private static final LocalDate TUESDAY = LocalDate.of(2026, 8, 25);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private ScheduleConfigService scheduleConfigService;

    @MockitoBean
    private HolidaySyncService holidaySyncService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User user(Role role) {
        return User.builder()
                .id(role == Role.PATIENT ? 1L : 2L)
                .email("user@example.com")
                .passwordHash("hashed")
                .firstName("Ana")
                .lastName("Gomez")
                .role(role)
                .build();
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    private ConfirmedSlot conflict() {
        return new ConfirmedSlot(7L, TUESDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "Ana Gomez");
    }

    @Test
    void getRange_asPatient_returnsLocalDates() throws Exception {
        when(availabilityService.getRange(any(), any())).thenReturn(List.of(new DayAvailability(TUESDAY, 4)));

        mockMvc.perform(get("/api/schedule/availability").with(authentication(authFor(user(Role.PATIENT)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-08-25"))
                .andExpect(jsonPath("$[0].freeSlots").value(4));
    }

    @Test
    void getDay_serializesHoursWithoutSeconds() throws Exception {
        when(availabilityService.getDay(TUESDAY)).thenReturn(new DaySchedule(TUESDAY,
                List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(10, 0), true))));

        mockMvc.perform(get("/api/schedule/availability/2026-08-25")
                        .with(authentication(authFor(user(Role.PATIENT)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-25"))
                .andExpect(jsonPath("$.slots[0].startTime").value("09:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("10:00"))
                .andExpect(jsonPath("$.slots[0].available").value(true));
    }

    @Test
    void getRange_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/schedule/availability"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSlots_asPatient_returns403() throws Exception {
        mockMvc.perform(get("/api/schedule/slots").with(authentication(authFor(user(Role.PATIENT)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBlock_overConfirmedAppointment_returns409WithTheAppointments() throws Exception {
        when(scheduleConfigService.createBlock(any())).thenThrow(
                new ConfirmedAppointmentConflictException("The block overlaps confirmed appointments",
                        List.of(conflict())));

        BlockedDateRequest request = new BlockedDateRequest(TUESDAY, TUESDAY, null, null, "Vacaciones", false);

        mockMvc.perform(post("/api/schedule/blocks")
                        .with(authentication(authFor(user(Role.PSYCHOLOGIST))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts[0].date").value("2026-08-25"))
                .andExpect(jsonPath("$.conflicts[0].startTime").value("12:00"))
                .andExpect(jsonPath("$.conflicts[0].patientFullName").value("Ana Gomez"));
    }

    @Test
    void createBlock_hoursOnAMultiDayRange_returns400() throws Exception {
        BlockedDateRequest request = new BlockedDateRequest(TUESDAY, TUESDAY.plusDays(3),
                LocalTime.of(15, 0), LocalTime.of(16, 0), "Tramite", false);

        mockMvc.perform(post("/api/schedule/blocks")
                        .with(authentication(authFor(user(Role.PSYCHOLOGIST))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteBlock_holidayBlock_returns409() throws Exception {
        doThrow(new HolidayBlockNotDeletableException(5L)).when(scheduleConfigService).deleteBlock(5L);

        mockMvc.perform(delete("/api/schedule/blocks/5").with(authentication(authFor(user(Role.PSYCHOLOGIST)))))
                .andExpect(status().isConflict());
    }
}
