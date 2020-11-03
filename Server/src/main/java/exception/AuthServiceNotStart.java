package exception;

public class AuthServiceNotStart extends RuntimeException {
    public AuthServiceNotStart(String message) {
        super(message);
    }
}
