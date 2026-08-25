package org.clinica.booking.schedule.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.clinica.booking.schedule.dto.AvailabilitySlotRequest;

public class TimeRangeValidator implements ConstraintValidator<ValidTimeRange, AvailabilitySlotRequest> {

    @Override
    public boolean isValid(AvailabilitySlotRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startTime() == null || request.endTime() == null) {
            return true;
        }
        return request.endTime().isAfter(request.startTime());
    }
}
