package luvoygroup.voyage.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Recurso não encontrado");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
