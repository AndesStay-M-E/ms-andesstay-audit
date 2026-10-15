package cl.duoc.andesstay.audit.audit.dto;

import cl.duoc.andesstay.audit.audit.AuditEvent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AuditEventResponse(
        Long id,
        UUID eventId,
        Long reservationId,
        String userEmail,
        Long accommodationId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String previousStatus,
        String newStatus,
        Instant occurredAt,
        Instant receivedAt,
        String traceId
) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventId(),
                event.getReservationId(),
                event.getUserEmail(),
                event.getAccommodationId(),
                event.getCheckInDate(),
                event.getCheckOutDate(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getOccurredAt(),
                event.getReceivedAt(),
                event.getTraceId()
        );
    }
}
