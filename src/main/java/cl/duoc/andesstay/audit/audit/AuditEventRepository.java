package cl.duoc.andesstay.audit.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findByEventId(UUID eventId);

    List<AuditEvent> findByReservationIdOrderByOccurredAtAsc(Long reservationId);
}
