package mtymes.common.domain;

import javafixes.object.DataObject;

import static com.google.common.base.Preconditions.checkNotNull;

public class Failure extends DataObject {

    public final String message;

    private Failure(String message) {
        // todo: test this
        checkNotNull(message, "message can't be null");
        this.message = message;
    }

    public static Failure failure(String value) {
        return new Failure(value);
    }
}
