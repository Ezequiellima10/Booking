package org.clinica.booking.patient.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.clinica.booking.auth.entity.Role;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.auth.repository.UserRepository;
import org.clinica.booking.patient.dto.PatientResponse;
import org.clinica.booking.patient.dto.UpdatePatientRequest;
import org.clinica.booking.patient.exception.PatientNotFoundException;
import org.clinica.booking.patient.mapper.PatientMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientServiceImpl implements PatientService {

    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getOwnProfile(Long userId) {
        return patientMapper.toResponse(findPatientOrThrow(userId));
    }

    @Override
    public PatientResponse updateOwnProfile(Long userId, UpdatePatientRequest request) {
        User patient = findPatientOrThrow(userId);
        patientMapper.applyUpdate(patient, request);
        log.info("Patient {} updated their profile", userId);
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> listPatients(Pageable pageable) {
        return userRepository.findByRole(Role.PATIENT, pageable).map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    private User findPatientOrThrow(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.PATIENT)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }
}
