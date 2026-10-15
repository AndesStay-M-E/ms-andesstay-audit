package cl.duoc.andesstay.audit.audit;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Filtros opcionales para consultar la auditoría.
 * Solo se agregan a la consulta los filtros que vienen informados.
 */
final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    static Specification<AuditEvent> withFilters(Long reservationId, String status, String userEmail) {
        Specification<AuditEvent> spec = (root, query, cb) -> cb.conjunction();

        if (reservationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reservationId"), reservationId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("newStatus"), status));
        }

        if (userEmail != null) {
            String email = userEmail.toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("userEmail")), email));
        }

        return spec;
    }
}
