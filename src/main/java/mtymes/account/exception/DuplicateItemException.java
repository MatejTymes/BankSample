package mtymes.account.exception;

public class DuplicateItemException extends RuntimeException {

    public DuplicateItemException(Throwable cause) {
        super(cause);
    }

    public DuplicateItemException() {
    }
}
