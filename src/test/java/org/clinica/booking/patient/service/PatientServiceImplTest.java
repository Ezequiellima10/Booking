package org.clinica.booking.patient.service;

import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.patient.dto.PatientResponse;
import org.clinica.booking.patient.dto.UpdatePatientRequest;
import org.clinica.booking.patient.exception.PatientNotFoundException;
import org.clinica.booking.patient.mapper.PatientMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

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

    @Test
    void getOwnProfile_existingPatient_returnsMappedResponse() {
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        PatientResponse result = patientService.getOwnProfile(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getOwnProfile_userNotFound_throwsPatientNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getOwnProfile(1L))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void getOwnProfile_userIsPsychologist_throwsPatientNotFoundException() {
        User psychologist = buildPsychologist();
        when(userRepository.findById(2L)).thenReturn(Optional.of(psychologist));

        assertThatThrownBy(() -> patientService.getOwnProfile(2L))
                .isInstanceOf(PatientNotFoundException.class);
    }

    @Test
    void updateOwnProfile_existingPatient_appliesUpdateAndReturnsResponse() {
        User patient = buildPatient();
        UpdatePatientRequest request = new UpdatePatientRequest("Ana", "Gomez", "123456789");
        PatientResponse response = buildResponse(patient);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        PatientResponse result = patientService.updateOwnProfile(1L, request);

        verify(patientMapper).applyUpdate(patient, request);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateOwnProfile_userNotFound_throwsAndDoesNotApplyUpdate() {
        UpdatePatientRequest request = new UpdatePatientRequest("Ana", "Gomez", null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.updateOwnProfile(1L, request))
                .isInstanceOf(PatientNotFoundException.class);

        verify(patientMapper, org.mockito.Mockito.never()).applyUpdate(any(), any());
    }

    @Test
    void listPatients_returnsPageMappedToResponse() {
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        Pageable pageable = Pageable.unpaged();
        when(userRepository.findByRole(Role.PATIENT, pageable)).thenReturn(new PageImpl<>(java.util.List.of(patient)));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        Page<PatientResponse> result = patientService.listPatients(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getPatientById_existingPatient_returnsMappedResponse() {
        User patient = buildPatient();
        PatientResponse response = buildResponse(patient);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        PatientResponse result = patientService.getPatientById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getPatientById_notFound_throwsPatientNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(99L))
                .isInstanceOf(PatientNotFoundException.class);
    }
}
