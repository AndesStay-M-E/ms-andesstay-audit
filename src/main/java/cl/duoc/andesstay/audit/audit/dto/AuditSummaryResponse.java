package cl.duoc.andesstay.audit.audit.dto;

import java.time.Instant;
import java.util.Map;

/**
 * totalEvents: cantidad de eventos registrados.
 * reservations: cantidad de reservas distintas con eventos.
 * eventsByStatus: eventos por estado nuevo (CREADA, CONFIRMADA, ...).
 */
public record AuditSummaryResponse(
        long totalEvents,
        long reservations,
        Map<String, Long> eventsByStatus,
        Instant lastEventAt
) {
}
