package luvoygroup.voyage.exception;

public class InsufficientContentException extends RuntimeException {
    public InsufficientContentException() {
        super("Destino com POIs insuficientes para gerar roteiro");
    }

    public InsufficientContentException(String message) {
        super(message);
    }
}
