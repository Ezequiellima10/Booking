package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.AvailabilitySlotResponse;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.BlockedDateResponse;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleConfigService {

    List<AvailabilitySlotResponse> listSlots();

    AvailabilitySlotResponse createSlot(AvailabilitySlotRequest request);

    AvailabilitySlotResponse updateSlot(Long id, AvailabilitySlotRequest request);

    void deleteSlot(Long id);

    List<BlockedDateResponse> listBlocks(LocalDate from, LocalDate to);

    BlockedDateResponse createBlock(BlockedDateRequest request);

    void deleteBlock(Long id);
}
