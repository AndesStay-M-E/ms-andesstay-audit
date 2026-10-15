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

## Ejecutar

1. Levantar RabbitMQ con el repositorio `infra-andesstay-messaging` (`docker compose up -d`).
2. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

## Pruebas

`./mvnw test`
