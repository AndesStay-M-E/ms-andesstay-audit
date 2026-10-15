package cl.duoc.andesstay.audit.messaging.model;

import java.time.LocalDate;

/**
 * Mismo JSON que ReservationStatusChangedEvent de reservations.
 * Los estados se reciben como texto para no acoplar este servicio
 * al enum del productor.
 */
public record ReservationStatusChangedPayload(
        Long reservationId,
        String userEmail,
        Long accommodationId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String previousStatus,
        String newStatus
) {
}
