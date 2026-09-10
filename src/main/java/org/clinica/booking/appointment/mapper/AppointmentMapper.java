package org.clinica.booking.appointment.mapper;

import org.clinica.booking.appointment.dto.AppointmentResponse;
import org.clinica.booking.appointment.dto.QueueEntry;
import org.clinica.booking.appointment.entity.Appointment;
import org.clinica.booking.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getRecurrenceGroup() == null ? null : appointment.getRecurrenceGroup().getId()
        );
    }

    public QueueEntry toQueueEntry(Appointment appointment) {
        return new QueueEntry(
                appointment.getId(),
                fullName(appointment.getPatient()),
                appointment.getStatus(),
                appointment.getCreatedAt()
        );
    }

    public String fullName(User patient) {
        return patient.getFirstName() + " " + patient.getLastName();
    }
}
