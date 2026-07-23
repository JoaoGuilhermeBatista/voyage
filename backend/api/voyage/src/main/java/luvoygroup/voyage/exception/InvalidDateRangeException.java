package luvoygroup.voyage.exception;

public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException() {
        super("Intervalo de datas inválido");
    }

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
