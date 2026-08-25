package org.clinica.booking.schedule.mapper;

import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;
import org.clinica.booking.schedule.dto.AvailabilitySlotResponse;
import org.clinica.booking.schedule.dto.BlockedDateRequest;
import org.clinica.booking.schedule.dto.BlockedDateResponse;
import org.clinica.booking.schedule.entity.AvailabilitySlot;
import org.clinica.booking.schedule.entity.BlockSource;
import org.clinica.booking.schedule.entity.BlockedDate;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

    public AvailabilitySlotResponse toResponse(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(
                slot.getId(),
                slot.getType(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getReason()
        );
    }

    public BlockedDateResponse toResponse(BlockedDate block) {
        return new BlockedDateResponse(
                block.getId(),
                block.getStartDate(),
                block.getEndDate(),
                block.getStartTime(),
                block.getEndTime(),
                block.getReason(),
                block.getSource()
        );
    }

    public AvailabilitySlot toEntity(AvailabilitySlotRequest request) {
        return AvailabilitySlot.builder()
                .type(request.type())
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .build();
    }

    public BlockedDate toEntity(BlockedDateRequest request) {
        return BlockedDate.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .source(BlockSource.MANUAL)
                .build();
    }

    public void applyUpdate(AvailabilitySlot slot, AvailabilitySlotRequest request) {
        slot.setType(request.type());
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setReason(request.reason());
    }
}
