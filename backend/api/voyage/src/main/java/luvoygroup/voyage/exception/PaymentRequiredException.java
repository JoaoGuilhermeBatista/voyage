package luvoygroup.voyage.exception;

public class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException() {
        super("Plano Premium necessário para esta ação");
    }
}
