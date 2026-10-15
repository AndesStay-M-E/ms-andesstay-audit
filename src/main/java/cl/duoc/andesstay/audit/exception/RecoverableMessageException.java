package cl.duoc.andesstay.audit.exception;

/**
 * Error temporal (servicio externo caído, timeout, etc.).
 * El mensaje se reintenta hasta max-attempts y luego va a la DLQ.
 */
public class RecoverableMessageException extends RuntimeException {

    public RecoverableMessageException(String message) {
        super(message);
    }
}
