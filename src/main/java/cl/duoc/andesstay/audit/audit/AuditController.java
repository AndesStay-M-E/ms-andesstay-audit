package cl.duoc.andesstay.audit.audit;

import cl.duoc.andesstay.audit.audit.dto.AuditEventResponse;
import cl.duoc.andesstay.audit.audit.dto.AuditSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/audit/events?reservationId=&status=&userEmail=&limit=100   consulta con filtros
 * GET /api/audit/reservations/{id}/timeline                           línea de tiempo de una reserva
 * GET /api/audit/summary                                              resumen por estado
 *
 * Requieren rol ADMIN o AUDITOR.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public List<AuditEventResponse> search(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userEmail,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return service.search(reservationId, status, userEmail, limit);
    }

    @GetMapping("/reservations/{reservationId}/timeline")
    public List<AuditEventResponse> timeline(@PathVariable Long reservationId) {
        return service.timeline(reservationId);
    }

    @GetMapping("/summary")
    public AuditSummaryResponse summary() {
        return service.summary();
    }
}
