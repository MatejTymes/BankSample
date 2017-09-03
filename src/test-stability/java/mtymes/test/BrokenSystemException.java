package mtymes.test;

public class BrokenSystemException extends RuntimeException {

    public BrokenSystemException() {
        super("something got broken");
    }
}
