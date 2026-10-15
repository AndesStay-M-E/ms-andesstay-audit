package cl.duoc.andesstay.audit.messaging.consumer;

import cl.duoc.andesstay.audit.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.audit.exception.RecoverableMessageException;
import cl.duoc.andesstay.audit.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.audit.messaging.model.ReservationStatusChangedPayload;
import cl.duoc.andesstay.audit.messaging.support.MessageAcknowledger;
import cl.duoc.andesstay.audit.audit.AuditService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola audit.reservation.q (routing key reservation.#).
 *
 * Solo traduce el mensaje y decide ACK / reintento / DLQ.
 * La lógica de negocio está en AuditService.
 */
@Component
public class ReservationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationEventListener.class);

    private final AuditService auditService;
    private final MessageAcknowledger acknowledger;

    public ReservationEventListener(
            AuditService auditService,
            MessageAcknowledger acknowledger
    ) {
        this.auditService = auditService;
        this.acknowledger = acknowledger;
    }

    @RabbitListener(
            id = "audit-audit-listener",
            queues = "${andesstay.messaging.route.queue}"
    )
    public void onAuditMessage(
            @Payload MessageEnvelope<ReservationStatusChangedPayload> envelope,
            Message message,
            Channel channel
    ) {
        String reference = envelope.reference();

        log.info(
                "Mensaje recibido type={} eventId={} correlationId={} intento={}",
                envelope.type(),
                reference,
                envelope.correlationId(),
                acknowledger.currentAttempt(message) + 1
        );

        try {
            auditService.process(envelope);
            acknowledger.ack(channel, message, reference);

        } catch (NonRecoverableMessageException exception) {
            acknowledger.deadLetter(channel, message, reference, exception.getMessage());

        } catch (RecoverableMessageException exception) {
            acknowledger.retryOrDeadLetter(channel, message, reference, exception.getMessage());

        } catch (RuntimeException exception) {
            // Error no previsto: se trata como temporal y se reintenta.
            acknowledger.retryOrDeadLetter(
                    channel,
                    message,
                    reference,
                    "Error inesperado: " + exception.getMessage()
            );
        }
    }
}
