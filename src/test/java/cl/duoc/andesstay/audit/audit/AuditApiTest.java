package cl.duoc.andesstay.audit.audit;

import cl.duoc.andesstay.audit.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.audit.messaging.model.ReservationStatusChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la API de auditoría con base H2 real y la seguridad por roles.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:auditapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AuditApiTest {

    private static final long RESERVATION = 501L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        auditService.process(event(null, "CREADA", "2026-10-15T10:00:00Z"));
        auditService.process(event("CREADA", "CONFIRMADA", "2026-10-15T11:00:00Z"));
        auditService.process(event("CONFIRMADA", "CANCELADA", "2026-10-15T12:00:00Z"));
    }

    @Test
    void auditorFiltraPorEstado() throws Exception {
        mockMvc.perform(get("/api/audit/events").param("status", "confirmada").with(role("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].newStatus").value("CONFIRMADA"));
    }

    @Test
    void timelineEnOrdenCronologico() throws Exception {
        mockMvc.perform(get("/api/audit/reservations/" + RESERVATION + "/timeline").with(role("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].newStatus").value("CREADA"))
                .andExpect(jsonPath("$[2].newStatus").value("CANCELADA"));
    }

    @Test
    void resumenCuentaPorEstado() throws Exception {
        mockMvc.perform(get("/api/audit/summary").with(role("AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(3))
                .andExpect(jsonPath("$.reservations").value(1))
                .andExpect(jsonPath("$.eventsByStatus.CANCELADA").value(1));
    }

    @Test
    void estadoInvalidoResponde400() throws Exception {
        mockMvc.perform(get("/api/audit/events").param("status", "PAGADA").with(role("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservaSinEventosResponde404() throws Exception {
        mockMvc.perform(get("/api/audit/reservations/999999/timeline").with(role("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void operadorNoTieneAcceso() throws Exception {
        mockMvc.perform(get("/api/audit/events").with(role("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/audit/summary"))
                .andExpect(status().isUnauthorized());
    }

    private MessageEnvelope<ReservationStatusChangedPayload> event(String previous, String next, String at) {
        return new MessageEnvelope<>(
                "reservation.status-changed",
                UUID.randomUUID(),
                Instant.parse(at),
                "trace-" + next,
                "reservation-" + RESERVATION,
                new ReservationStatusChangedPayload(
                        RESERVATION, "cliente@andesstay.cl", 1L,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), previous, next)
        );
    }

    private JwtRequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
