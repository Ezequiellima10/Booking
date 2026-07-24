package org.clinica.booking.patient.service;

import org.clinica.booking.patient.dto.PatientResponse;
import org.clinica.booking.patient.dto.UpdatePatientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse getOwnProfile(Long userId);

    PatientResponse updateOwnProfile(Long userId, UpdatePatientRequest request);

    Page<PatientResponse> listPatients(Pageable pageable);

    PatientResponse getPatientById(Long id);
}