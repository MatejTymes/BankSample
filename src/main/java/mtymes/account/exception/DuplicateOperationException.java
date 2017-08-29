package mtymes.account.exception;

public class DuplicateOperationException extends RuntimeException {

    public DuplicateOperationException(Throwable cause) {
        super(cause);
    }

    public DuplicateOperationException() {
    }
}
