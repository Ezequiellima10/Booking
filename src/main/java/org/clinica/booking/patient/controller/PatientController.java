package org.clinica.booking.patient.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clinica.booking.auth.entity.User;
import org.clinica.booking.patient.dto.PatientResponse;
import org.clinica.booking.patient.dto.UpdatePatientRequest;
import org.clinica.booking.patient.service.PatientService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/me")
    public PatientResponse getOwnProfile(@AuthenticationPrincipal User currentUser) {
        return patientService.getOwnProfile(currentUser.getId());
    }

    @PatchMapping("/me")
    public PatientResponse updateOwnProfile(@AuthenticationPrincipal User currentUser,
                                             @Valid @RequestBody UpdatePatientRequest request) {
        return patientService.updateOwnProfile(currentUser.getId(), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public Page<PatientResponse> listPatients(Pageable pageable) {
        return patientService.listPatients(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PSYCHOLOGIST')")
    public PatientResponse getPatientById(@PathVariable Long id) {
        return patientService.getPatientById(id);
    }
}