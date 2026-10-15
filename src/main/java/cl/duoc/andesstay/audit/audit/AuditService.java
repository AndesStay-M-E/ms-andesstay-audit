package cl.duoc.andesstay.audit.audit;

import cl.duoc.andesstay.audit.audit.dto.AuditEventResponse;
import cl.duoc.andesstay.audit.audit.dto.AuditSummaryResponse;
import cl.duoc.andesstay.audit.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.audit.exception.ResourceNotFoundException;
import cl.duoc.andesstay.audit.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.audit.messaging.model.ReservationStatusChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registro y consulta de la auditoría de reservas.
 *
 * process() se invoca desde el consumidor RabbitMQ; el resto, desde la API REST.
 */
@Service
@Transactional
public class AuditService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditService.class);

    /** Estados válidos de una reserva, en el orden del flujo. */
    public static final List<String> STATUSES = List.of(
            "CREADA", "CONFIRMADA", "CHECKIN_PENDIENTE", "EN_ESTADIA", "CHECKOUT", "CANCELADA");

    private static final Set<String> VALID_STATUSES = Set.copyOf(STATUSES);

    static final int MAX_LIMIT = 500;

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Guarda el evento. Si ya fue registrado (mismo eventId), no lo duplica.
     *
     * Errores de datos -> NonRecoverableMessageException (va a la DLQ).
     * Errores de base de datos -> se propagan y el consumidor reintenta.
     */
    public void process(MessageEnvelope<ReservationStatusChangedPayload> envelope) {
        ReservationStatusChangedPayload payload = envelope.requirePayload();

        if (envelope.eventId() == null) {
            throw new NonRecoverableMessageException("El evento no trae eventId");
        }

        if (payload.reservationId() == null || payload.newStatus() == null) {
            throw new NonRecoverableMessageException("Evento de auditoría sin reserva o sin estado nuevo");
        }

        if (!VALID_STATUSES.contains(payload.newStatus())) {
            throw new NonRecoverableMessageException("Estado de reserva desconocido: " + payload.newStatus());
        }

        if (repository.findByEventId(envelope.eventId()).isPresent()) {
            log.warn("Evento {} ya registrado en auditoría", envelope.reference());
            return;
        }

        AuditEvent event = new AuditEvent();
        event.setEventId(envelope.eventId());
        event.setEventType(envelope.type());
        event.setReservationId(payload.reservationId());
        event.setUserEmail(payload.userEmail());
        event.setAccommodationId(payload.accommodationId());
        event.setCheckInDate(payload.checkInDate());
        event.setCheckOutDate(payload.checkOutDate());
        event.setPreviousStatus(payload.previousStatus());
        event.setNewStatus(payload.newStatus());
        event.setOccurredAt(envelope.timestamp());
        event.setTraceId(envelope.traceId());
        event.setCorrelationId(envelope.correlationId());

        AuditEvent saved = repository.save(event);

        log.info(
                "AUDITORIA id={} reserva={} estado={} -> {} usuario={} fecha={} eventId={} traceId={}",
                saved.getId(),
                saved.getReservationId(),
                saved.getPreviousStatus() != null ? saved.getPreviousStatus() : "(nueva)",
                saved.getNewStatus(),
                saved.getUserEmail(),
                saved.getOccurredAt(),
                envelope.reference(),
                envelope.traceId()
        );
    }

    /**
     * Consulta con filtros opcionales, del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> search(Long reservationId, String status, String userEmail, int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y " + MAX_LIMIT);
        }

        String normalizedStatus = normalizeStatus(status);
        String normalizedEmail = userEmail == null || userEmail.isBlank() ? null : userEmail.trim();

        return repository.findAll(
                        AuditEventSpecifications.withFilters(reservationId, normalizedStatus, normalizedEmail),
                        PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "occurredAt", "id")))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    /**
     * Línea de tiempo de una reserva, en orden cronológico.
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> timeline(Long reservationId) {
        List<AuditEventResponse> events = repository.findByReservationIdOrderByOccurredAtAsc(reservationId)
                .stream()
                .map(AuditEventResponse::from)
                .toList();

        if (events.isEmpty()) {
            throw new ResourceNotFoundException("No hay eventos de auditoría para la reserva " + reservationId);
        }

        return events;
    }

    @Transactional(readOnly = true)
    public AuditSummaryResponse summary() {
        List<AuditEvent> all = repository.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        STATUSES.forEach(status -> byStatus.put(status, 0L));
        all.forEach(event -> byStatus.merge(event.getNewStatus(), 1L, Long::sum));

        long reservations = all.stream()
                .map(AuditEvent::getReservationId)
                .collect(Collectors.toSet())
                .size();

        Instant lastEventAt = all.stream()
                .map(AuditEvent::getOccurredAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);

        return new AuditSummaryResponse(all.size(), reservations, byStatus, lastEventAt);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);

        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Estado inválido: " + status + ". Valores permitidos: " + String.join(", ", STATUSES));
        }

        return normalized;
    }
}
