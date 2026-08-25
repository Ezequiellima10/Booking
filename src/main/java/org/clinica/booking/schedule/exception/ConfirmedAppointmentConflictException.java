package org.clinica.booking.schedule.exception;

import lombok.Getter;
import org.clinica.booking.schedule.dto.ConfirmedSlot;

import java.util.List;

@Getter
public class ConfirmedAppointmentConflictException extends RuntimeException {

    private final transient List<ConfirmedSlot> conflicts;

    public ConfirmedAppointmentConflictException(String message, List<ConfirmedSlot> conflicts) {
        super(message);
        this.conflicts = List.copyOf(conflicts);
    }
}
