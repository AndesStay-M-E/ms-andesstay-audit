package cl.duoc.andesstay.audit.audit;

import cl.duoc.andesstay.audit.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.audit.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.audit.messaging.model.ReservationStatusChangedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Registra en el log de auditoría cada cambio de estado de una reserva.
 */
@Service
public class AuditService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditService.class);

    public void process(MessageEnvelope<ReservationStatusChangedPayload> envelope) {
        ReservationStatusChangedPayload payload = envelope.requirePayload();

        if (payload.reservationId() == null || payload.newStatus() == null) {
            throw new NonRecoverableMessageException(
                    "Evento de auditoría sin reserva o sin estado nuevo"
            );
        }

        String previous = payload.previousStatus() != null
                ? payload.previousStatus()
                : "(nueva)";

        log.info(
                "AUDITORIA reserva={} estado={} -> {} usuario={} fecha={} eventId={} traceId={}",
                payload.reservationId(),
                previous,
                payload.newStatus(),
                payload.userEmail(),
                envelope.timestamp(),
                envelope.reference(),
                envelope.traceId()
        );
    }
}
