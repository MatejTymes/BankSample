package mtymes.account.domain.operation;

import com.fasterxml.jackson.annotation.JsonCreator;
import javafixes.object.Microtype;

import java.util.UUID;

public class OperationId extends Microtype<UUID> {

    private OperationId(UUID value) {
        super(value);
    }

    public static OperationId operationId(UUID value) {
        return new OperationId(value);
    }

    @JsonCreator
    public static OperationId operationId(String value) {
        return new OperationId(UUID.fromString(value));
    }
}
