package luvoygroup.voyage.exception;

public record ErrorResponse(String error, String message, int status) {
}
