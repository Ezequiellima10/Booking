package org.clinica.booking.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.AvailabilitySlotResponse;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.BlockedDateResponse;
import org.clinica.booking.schedule.dto.HolidayConflict;
import org.clinica.booking.schedule.service.HolidaySyncService;
import org.clinica.booking.schedule.service.ScheduleConfigService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PSYCHOLOGIST')")
public class ScheduleConfigController {

    private final ScheduleConfigService scheduleConfigService;
    private final HolidaySyncService holidaySyncService;

    @GetMapping("/slots")
    public List<AvailabilitySlotResponse> listSlots() {
        return scheduleConfigService.listSlots();
    }

    @PostMapping("/slots")
    public ResponseEntity<AvailabilitySlotResponse> createSlot(@Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleConfigService.createSlot(request));
    }

    @PutMapping("/slots/{id}")
    public AvailabilitySlotResponse updateSlot(@PathVariable Long id,
                                               @Valid @RequestBody AvailabilitySlotRequest request) {
        return scheduleConfigService.updateSlot(id, request);
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        scheduleConfigService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocks")
    public List<BlockedDateResponse> listBlocks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleConfigService.listBlocks(from, to);
    }

    @PostMapping("/blocks")
    public ResponseEntity<BlockedDateResponse> createBlock(@Valid @RequestBody BlockedDateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleConfigService.createBlock(request));
    }

    @DeleteMapping("/blocks/{id}")
    public ResponseEntity<Void> deleteBlock(@PathVariable Long id) {
        scheduleConfigService.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/holiday-conflicts")
    public List<HolidayConflict> findHolidayConflicts() {
        return holidaySyncService.findHolidayConflicts();
    }
}
