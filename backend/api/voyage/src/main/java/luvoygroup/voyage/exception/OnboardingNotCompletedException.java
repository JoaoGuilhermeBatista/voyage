package luvoygroup.voyage.exception;

public class OnboardingNotCompletedException extends RuntimeException {
    public OnboardingNotCompletedException() {
        super("Onboarding não concluído");
    }
}
