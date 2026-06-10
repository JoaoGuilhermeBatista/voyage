package luvoygroup.voyage.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Email já cadastrado");
    }
}
