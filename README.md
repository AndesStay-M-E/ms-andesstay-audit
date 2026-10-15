# ms-andesstay-audit

Microservicio de auditoría: consume todos los eventos de reservas.

| Elemento | Valor |
|---|---|
| Puerto | 8085 |
| Exchange origen | `andesstay.events` (topic) |
| Routing key | `reservation.#` |
| Cola | `audit.reservation.q` |
| DLQ | `audit.reservation.dlq` (vía `andesstay.dlx`) |

## Manejo de mensajes

- ACK manual (`acknowledge-mode: manual`).
- Error no recuperable (datos inválidos): NACK sin reencolar, el mensaje va a la DLQ y se registra en el log.
- Error recuperable: se reintenta hasta `andesstay.messaging.retry.max-attempts` (3) y luego va a la DLQ.
- JSON inválido: el contenedor lo rechaza sin reencolar (`default-requeue-rejected: false`) y va a la DLQ.

## Base de datos

- Perfil `local` (por defecto): H2 en `./data/audit`.
- Perfil `cloud`: AWS RDS por variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Tabla: `audit_events` (índices por reserva y por fecha).

## API REST (JWT de Azure AD, roles ADMIN o AUDITOR)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/audit/events?reservationId=&status=&userEmail=&limit=100` | Consulta con filtros opcionales (400 si el estado o el límite no son válidos) |
| GET | `/api/audit/reservations/{id}/timeline` | Línea de tiempo de una reserva (404 si no tiene eventos) |
| GET | `/api/audit/summary` | Total de eventos, reservas auditadas y eventos por estado |

Cada evento `reservation.*` recibido por RabbitMQ se guarda una sola vez (por `eventId`).

## Ejecutar

1. Levantar RabbitMQ con el repositorio `infra-andesstay-messaging` (`docker compose up -d`).
2. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

## Pruebas

`./mvnw test`
