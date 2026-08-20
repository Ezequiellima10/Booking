package org.clinica.booking.schedule.service;

import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockedDate;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleConfigService {

    List<AvailabilitySlot> listSlots();

    AvailabilitySlot createSlot(AvailabilitySlotRequest request);

    AvailabilitySlot updateSlot(Long id, AvailabilitySlotRequest request);

    void deleteSlot(Long id);

    List<BlockedDate> listBlocks(LocalDate from, LocalDate to);

    BlockedDate createBlock(BlockedDateRequest request);

    void deleteBlock(Long id);
}
