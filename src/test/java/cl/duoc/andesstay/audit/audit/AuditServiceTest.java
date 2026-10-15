package cl.duoc.andesstay.audit.audit;

import cl.duoc.andesstay.audit.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.audit.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.audit.messaging.model.ReservationStatusChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private AuditEventRepository repository;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditEventRepository.class);
        when(repository.save(any(AuditEvent.class))).thenAnswer(call -> call.getArgument(0));
        service = new AuditService(repository);
    }

    @Test
    void guardaElEventoConSuTrazabilidad() {
        MessageEnvelope<ReservationStatusChangedPayload> envelope = envelope("CREADA", "CONFIRMADA");
        when(repository.findByEventId(envelope.eventId())).thenReturn(Optional.empty());

        service.process(envelope);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());
        AuditEvent saved = captor.getValue();
        assertThat(saved.getReservationId()).isEqualTo(7L);
        assertThat(saved.getPreviousStatus()).isEqualTo("CREADA");
        assertThat(saved.getNewStatus()).isEqualTo("CONFIRMADA");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getOccurredAt()).isEqualTo(envelope.timestamp());
    }

    @Test
    void noDuplicaUnEventoYaRegistrado() {
        MessageEnvelope<ReservationStatusChangedPayload> envelope = envelope(null, "CREADA");
        when(repository.findByEventId(envelope.eventId())).thenReturn(Optional.of(new AuditEvent()));

        service.process(envelope);

        verify(repository, never()).save(any());
    }

    @Test
    void estadoDesconocidoEsNoRecuperable() {
        assertThatThrownBy(() -> service.process(envelope("CREADA", "PAGADA")))
                .isInstanceOf(NonRecoverableMessageException.class);
    }

    @Test
    void filtrosInvalidosSonRechazados() {
        assertThatThrownBy(() -> service.search(null, "PAGADA", null, 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.search(null, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.search(null, null, null, AuditService.MAX_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private MessageEnvelope<ReservationStatusChangedPayload> envelope(String previous, String next) {
        return new MessageEnvelope<>(
                "reservation.status-changed",
                UUID.randomUUID(),
                Instant.parse("2026-10-15T15:00:00Z"),
                "trace-1",
                "reservation-7",
                new ReservationStatusChangedPayload(
                        7L, "cliente@andesstay.cl", 3L,
                        LocalDate.now(), LocalDate.now().plusDays(2), previous, next)
        );
    }
}
