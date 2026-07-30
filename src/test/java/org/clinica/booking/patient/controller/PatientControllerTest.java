package org.clinica.booking.patient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.auth.service.JwtService;
import org.clinica.booking.config.SecurityConfig;
import org.clinica.booking.patient.dto.PatientResponse;
import org.clinica.booking.patient.dto.UpdatePatientRequest;
import org.clinica.booking.patient.exception.PatientNotFoundException;
import org.clinica.booking.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)


@Import(SecurityConfig.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private User buildPatient() {
        return User.builder()
                .id(1L)
                .email("patient@example.com")
                .passwordHash("hashed")
                .firstName("Ana")
                .lastName("Gomez")
                .role(Role.PATIENT)
                .build();
    }

    private User buildPsychologist() {
        return User.builder()
                .id(2L)
                .email("psych@example.com")
                .passwordHash("hashed")
                .firstName("Marco")
                .lastName("Diaz")
                .role(Role.PSYCHOLOGIST)
                .build();
    }

    private PatientResponse buildResponse(User user) {
        return new PatientResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhone(), OffsetDateTime.now());
    }

    private UsernamePasswordAuthenticationToken authFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Test
    void getOwnProfile_authenticatedPatient_returns200() throws Exception {
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        when(patientService.getOwnProfile(1L)).thenReturn(response);

        mockMvc.perform(get("/api/patients/me").with(authentication(authFor(patient))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"));
    }

    @Test
    void getOwnProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateOwnProfile_validRequest_returns200() throws Exception {
        User patient = buildPatient();
        UpdatePatientRequest request = new UpdatePatientRequest("Ana", "Gomez", "123456789");
        PatientResponse response = buildResponse(patient);
        when(patientService.updateOwnProfile(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/patients/me")
                        .with(authentication(authFor(patient)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"));
    }

    @Test
    void updateOwnProfile_blankFields_returns400WithFieldErrors() throws Exception {
        User patient = buildPatient();
        UpdatePatientRequest request = new UpdatePatientRequest("", "", null);

        mockMvc.perform(patch("/api/patients/me")
                        .with(authentication(authFor(patient)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists());
    }

    @Test
    void listPatients_asPsychologist_returns200() throws Exception {
        User psychologist = buildPsychologist();
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        when(patientService.listPatients(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/patients").with(authentication(authFor(psychologist))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("patient@example.com"));
    }

    @Test
    void listPatients_asPatient_returns403() throws Exception {
        User patient = buildPatient();

        mockMvc.perform(get("/api/patients").with(authentication(authFor(patient))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPatientById_asPsychologist_returns200() throws Exception {
        User psychologist = buildPsychologist();
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        when(patientService.getPatientById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/patients/1").with(authentication(authFor(psychologist))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("patient@example.com"));
    }

    @Test
    void getPatientById_asPatient_returns403() throws Exception {
        User patient = buildPatient();

        mockMvc.perform(get("/api/patients/1").with(authentication(authFor(patient))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPatientById_notFound_returns404() throws Exception {
        User psychologist = buildPsychologist();
        when(patientService.getPatientById(99L)).thenThrow(new PatientNotFoundException(99L));

        mockMvc.perform(get("/api/patients/99").with(authentication(authFor(psychologist))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Patient not found: 99"));
    }
}
