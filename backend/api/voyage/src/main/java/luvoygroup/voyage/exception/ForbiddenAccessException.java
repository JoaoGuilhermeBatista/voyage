package luvoygroup.voyage.exception;

public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("Acesso não autorizado a este recurso");
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
