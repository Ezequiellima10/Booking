package org.clinica.booking.schedule.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.clinica.booking.schedule.dto.BlockedDateRequest;

public class BlockedDateRangeValidator implements ConstraintValidator<ValidBlockedDateRange, BlockedDateRequest> {

    @Override
    public boolean isValid(BlockedDateRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            return true;
        }
        if (request.endDate().isBefore(request.startDate())) {
            return reject(context, "endDate must not be before startDate");
        }
        boolean hasStart = request.startTime() != null;
        boolean hasEnd = request.endTime() != null;
        if (hasStart != hasEnd) {
            return reject(context, "startTime and endTime must be provided together");
        }
        if (!hasStart) {
            return true;
        }
        if (!request.startDate().equals(request.endDate())) {
            return reject(context, "A block with hours must span a single day");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            return reject(context, "endTime must be after startTime");
        }
        return true;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
