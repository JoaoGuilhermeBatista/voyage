package luvoygroup.voyage.exception;

public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException() {
        super("Intervalo de datas inválido");
    }
}
